/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.exoplatform.task.service.impl;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.task.dao.DAOHandler;
import org.exoplatform.task.domain.Comment;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.dto.CommentDto;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.CommentService;
import org.exoplatform.task.storage.CommentStorage;
import org.exoplatform.task.storage.TaskStorage;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommentServiceImpl implements CommentService {
    private static final Log LOG = ExoLogger.getExoLogger(CommentServiceImpl.class);
    private static final Pattern pattern = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");

    @Inject
    private TaskStorage taskStorage;

    @Inject
    private CommentStorage commentStorage;

    @Inject
    private DAOHandler daoHandler;

    private ListenerService listenerService;

    public CommentServiceImpl(TaskStorage taskStorage, CommentStorage commentStorage, DAOHandler daoHandler, ListenerService listenerService) {
        this.taskStorage = taskStorage;
        this.commentStorage = commentStorage;
        this.daoHandler = daoHandler;
        this.listenerService = listenerService;
    }

    @Override
    public CommentDto getComment(long commentId) {
        CommentDto comment = commentStorage.getComment(commentId);
        comment.setComment(substituteUsernames(comment.getComment()));
        return comment;
    }

    @Override
    public List<CommentDto> getComments(long taskId, int offset, int limit) {
        return commentStorage.getComments(taskId,offset,limit);
    }

    @Override
    public List<CommentDto> getCommentsWithSubs(long taskId, int offset, int limit){
        return commentStorage.getCommentsWithSubs(taskId,offset,limit);
    }

    @Override
    public int countComments(long taskId) {
        return commentStorage.countComments(taskId);
    }

    @Override
    public List<CommentDto> loadSubComments(List<CommentDto> listComments) {
        if (listComments == null || listComments.isEmpty()) {
            return null;
        }
        listComments.forEach(comment -> comment.setComment(substituteUsernames(comment.getComment())));
        List<CommentDto> subComments = commentStorage.loadSubComments(listComments);
        for (CommentDto comment : listComments) {
            subComments.forEach(subComment -> subComment.setComment(substituteUsernames(subComment.getComment())));
            comment.setSubComments(subComments.stream()
                    .filter(subComment -> subComment.getParentComment().getId() == comment.getId())
                    .collect(Collectors.toList()));
        }
        return listComments;
    }

    @Override
    @ExoTransactional
    public CommentDto addComment(TaskDto task, long parentCommentId, String username, String comment) throws EntityNotFoundException {


        CommentDto commentDto = commentStorage.addComment(task,parentCommentId,username,comment);

        try {
            listenerService.broadcast(TASK_COMMENT_CREATION, commentDto.getTask(), commentDto);
            if(commentDto.getTask().getStatus()!=null && commentDto.getTask().getStatus().getProject() != null){
                listenerService.broadcast("exo.project.projectModified", null, commentDto.getTask().getStatus().getProject() );
            }
        } catch (Exception e) {
            LOG.error("Error while broadcasting task creation event", e);
        }

        return commentDto;
    }

    @Override
    @ExoTransactional
    public CommentDto addComment(TaskDto task, String username, String comment) throws EntityNotFoundException {
        return addComment(task, 0, username, comment);
    }

    @Override
    @ExoTransactional
    public void removeComment(long commentId) throws EntityNotFoundException {

        CommentDto comment = commentStorage.getComment(commentId);

        if (comment == null) {
            LOG.info("Can not find comment with ID: " + commentId);
            throw new EntityNotFoundException(commentId, CommentDto.class);
        }

        commentStorage.removeComment(commentId);
    }

    private String substituteUsernames(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }
        //
        Matcher matcher = pattern.matcher(message);

        // Replace all occurrences of pattern in input
        StringBuffer buf = new StringBuffer();
        while (matcher.find()) {
            // Get the match result
            String username = matcher.group().substring(1);
            if (username == null || username.isEmpty()) {
                continue;
            }
            Identity identity = LinkProvider.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, username, false);
            if (identity == null || identity.isDeleted() || !identity.isEnable()) {
                continue;
            }
            try {
                username = LinkProvider.getProfileLink(username, "dw");
            } catch (Exception e) {
                continue;
            }
            // Insert replacement
            if (username != null) {
                matcher.appendReplacement(buf, username);
            }
        }
        if (buf.length() > 0) {
            matcher.appendTail(buf);
            return buf.toString();
        }
        return message;
    }
}
