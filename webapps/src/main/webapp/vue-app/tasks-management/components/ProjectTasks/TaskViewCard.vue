<!--
  This file is part of the Meeds project (https://meeds.io/).
  Copyright (C) 2022 Meeds Association
  contact@meeds.io
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 3 of the License, or (at your option) any later version.
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.
  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->
<template>
  <v-app
    id="taskCardItem"
    class="taskBoardCardItem"
    :class="removeCompletedTask && 'completedTask' || ''">
    <v-hover v-slot="{ hover }">
      <v-card
        :class="[taskPriorityColor]"
        :elevation="hover && !isMobile && 4 || 0"
        class="taskCard taskViewCard pa-2"
        flat>
        <div class="taskTitleId  d-flex justify-space-between">
          <div class="taskCheckBox">
            <v-switch
              ref="autoFocusInput2"
              class="d-none"
              true-value="true"
              false-value="false" />
            <i
              :title="$t(taskCompletedTitle)"
              :class="taskCompletedClass" 
              @click="updateCompleted"></i>
          </div>
          <div class="taskTitle d-flex align-start" @click="openTaskDrawer()">
            <a
              ref="tooltip"
              :class="getTitleTaskClass()"
              :title="task.task.title"
              class="taskCardViewTitle">
              <span class="taskTitleEllipsis">{{ task.task.title }}</span>
            </a>
          </div>
        </div>
        <v-divider v-if="displayCardBottomSection" />
        <div
          v-if="displayCardBottomSection"
          class="taskActionsAndDate d-flex align-center justify-space-between">
          <div
            class="taskActionsAndLabels d-flex align-center">
            <div
              v-if="assigneeAndCoworkerArray && assigneeAndCoworkerArray.length"
              class="taskWorker  justify-space-between pe-2">
              <div
                :class="assigneeAndCoworkerArray && !assigneeAndCoworkerArray.length && task && task.labels && !task.labels.length && 'hideTaskAssignee'"
                class="taskAssignee d-flex flex-nowrap position-relative">
                <exo-user-avatars-list
                  :users="avatarToDisplay"
                  :max="1"
                  :icon-size="26"
                  retrieve-extra-information
                  avatar-overlay-position
                  @open-detail="$root.$emit('displayTasksAssigneeAndCoworker', avatarToDisplay)" />
              </div>
            </div>
            <div
              v-if="task.commentCount"
              class="taskComment d-flex align-center pe-2"
              @click="openTaskDrawer()">
              <i class="far fa-comment uiCommentIcon"></i>
              <span class="taskCommentNumber caption">{{ task.commentCount }}</span>
            </div>
            <div
              v-if="task.labels && task.labels.length"
              :class="getClassLabels()"
              class="text-truncate rounded"
              @click="openTaskDrawer()">
              <v-chip
                v-if="task.labels && task.labels.length == 1"
                :color="task.labels[0].color"
                :title="task.labels[0].name"
                class="mx-1 font-weight-bold theme--light"
                label
                small>
                <span class="text-truncate">
                  {{ task.labels[0].name }}
                </span>
              </v-chip>
              <div
                v-else-if="task.labels && task.labels.length > 1"
                :title="getLabelsList(task.labels)"
                class="taskTags d-flex align-center theme--light">
                <i class="uiIcon uiTagIcon me-1"></i>
                <span class="taskAttachNumber caption">{{ task.labels.length }}</span>
              </div>
            </div>
          </div>
          <div
            v-if="taskDueDate"
            class="taskStatusAndDate"
            @click="openTaskDrawer()">
            <div class="taskDueDate" :class="getOverdueTask(taskDueDate) ? 'red--text' : ''">
              <div>
                <date-format :value="taskDueDate" :format="dateTimeFormat" />
              </div>
            </div>
          </div>
        </div>
      </v-card>
    </v-hover>
  </v-app>
</template>
<script>
export default {
  props: {
    task: {
      type: Object,
      default: () => ({}),
    },
    showCompletedTasks: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      user: {},
      dateTimeFormat: {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
      },
      assigneeAndCoworkerArray: [],
      isPersonnalTask: this.task.task.status === null,
      drawer: null,
    };
  },
  computed: {
    taskDueDate() { 
      if (this.task?.task?.dueDate?.time) {
        const formattedDate = `${this.task.task.dueDate.year + 1900}/${this.task.task.dueDate.month + 1}/${this.task.task.dueDate.date}`;
        return new Date(formattedDate);
      }
      return null;
    },
    avatarToDisplay () {
      const usersList = [];
      if (this.assigneeAndCoworkerArray && this.assigneeAndCoworkerArray.length ) {
        this.assigneeAndCoworkerArray.forEach(user => {
          if (!usersList.includes({'userName': user.username})) {
            usersList.push({'userName': user.username});
          }
        });
      }
      return usersList;
    },
    displayCardBottomSection() {
      return this.taskDueDate || (this.task.labels && this.task.labels.length) || (this.assigneeAndCoworkerArray && this.assigneeAndCoworkerArray.length) || this.task.commentCount;
    },
    taskCompletedClass() {
      return this.task.task.completed === true ? 'uiIconValidate' : 'uiIconCircle';
    },
    taskCompletedTitle() {
      return this.task.task.completed === true ? 'message.markAsUnCompleted' : 'message.markAsCompleted';
    },
    taskPriorityColor() {
      switch (this.task.task.priority) {
      case 'HIGH':
        return 'taskHighPriority';
      case 'NORMAL':
        return 'taskNormalPriority';
      case 'LOW':
        return 'taskLowPriority';
      default:
        return 'taskNonePriority';
      }
    },
    removeCompletedTask() {
      return this.task.task.completed === true && !this.showCompletedTasks;
    },
    isMobile() {
      return this.$vuetify.breakpoint.mdAndDown;
    },
  },
  watch: {
    'task.assignee'() {
      this.getTaskAssigneeAndCoworkers();
    },
    'task.coworker'() {
      this.getTaskAssigneeAndCoworkers();
    },
  },
  created() {
    this.$root.$on('task-updated', task => {
      if (this.task?.task?.id === task?.id) {
        this.task.task = task;
      }
    });
    this.$root.$on('update-completed-task', (value, id) => {
      if (this.task.id === id) {
        this.task.task.completed = value;
        if (this.task.task.completed === true && !this.showCompletedTasks) {
          this.$emit('update-task-completed', this.task.task);
        }
      }
    });
    this.$root.$on('update-task-assignee',(value,id) => {
      this.updateTaskAssignee(value,id);
    });
    this.$root.$on('update-remove-task-labels',(value,id)=>{
      this.updateRemoveTaskLabels(value,id);
    });
    this.$root.$on('update-task-labels',(value,id)=>{
      this.updateTaskLabels(value,id);
    });
    this.$root.$on('update-task-comments',(value,id)=>{
      this.updateTaskComments(value,id);
    });
    this.$root.$on('update-task-coworker',(value,id)=>{
      this.updateTaskCoworker(value,id);
    });
    this.getTaskAssigneeAndCoworkers();
  },
  methods: {
    updateRemoveTaskLabels(value,id){
      if (this.task.id === id){
        this.task.labels=this.task.labels.filter(item => item.id !== value);
      }
    },
    updateTaskLabels(value,id){
      if (this.task.id === id){
        this.task.labels.push(value);
      }
    },
    updateTaskComments(value,id){
      if (this.task.id === id){
        this.task.commentCount=value;
      }
    },
    updateTaskCoworker(value,id){
      const coworkers = value.coworker;
      if (this.task.id === id){
        if (coworkers && coworkers.length) {
          this.task.coworker=[];
          coworkers.forEach((coworker) => {
            this.$identityService.getIdentityByProviderIdAndRemoteId('organization',coworker).then(user => {
              const taskCoworker = {
                id: `organization:${user.remoteId}`,
                username: user.remoteId,
                providerId: 'organization',
                displayName: user.profile.fullname,
                avatar: user.profile.avatar,
              };
              if (taskCoworker && !this.task.coworker.includes(taskCoworker)){
                this.task.coworker.push(taskCoworker);
              }
            });
          });
        } else {
          this.task.coworker = [];
        }
        this.$root.$emit('task-assignee-coworker-updated', this.task);
      }
    },
    updateTaskAssignee(value,id){

      if (this.task.id === id){
        if (value===null){
          this.task.assignee = null;
        } else {
          this.$identityService.getIdentityByProviderIdAndRemoteId('organization',value).then(user => {
            this.task.assignee = {
              id: `organization:${user.remoteId}`,
              username: user.remoteId,
              providerId: 'organization',
              displayName: user.profile.fullname,
              avatar: user.profile.avatar,
            };
          });
        }
        this.$root.$emit('task-assignee-coworker-updated', this.task);
      }
    },
    getTaskAssigneeAndCoworkers() {
      this.assigneeAndCoworkerArray=[];
      if (this.task.assignee && !this.assigneeAndCoworkerArray.includes(this.task.assignee)) {
        this.assigneeAndCoworkerArray.push(this.task.assignee);
      }
      if (this.task.coworker || this.task.coworker.length > 0) {
        this.task.coworker.forEach((coworker) => {
          if (coworker && !this.assigneeAndCoworkerArray.includes(coworker)){
            this.assigneeAndCoworkerArray.push(coworker);
          }
        });
      }
    },
    getLabelsList(taskLabels) {
      if (taskLabels.length > 1) {
        let labelText = '';
        taskLabels.forEach((label) => {
          labelText += `${label.name}\r\n`;
        });
        return labelText;
      }
    },
    getAssigneeAndCoworkerList(assigneeAndCoworker) {
      if (assigneeAndCoworker.length > 1) {
        let assigneeAndCoworkerText = '';
        assigneeAndCoworker.forEach((label) => {
          assigneeAndCoworkerText += `${label.displayName}\r\n`;
        });
        return assigneeAndCoworkerText;
      }
    },
    openTaskDrawer() {
      this.$root.$emit('open-task-drawer', this.task.task,);
    },

    onCloseDrawer: function (drawer) {
      this.drawer = drawer;
    },
    updateCompleted() {
      const task = {
        id: this.task.task.id,
        isCompleted: !this.task.task.completed,
      };
      
      if (task.id) {
        return this.$tasksService.updateCompleted(task).then(updatedTask => {
          if (updatedTask.completed) {
            this.$root.$emit('show-alert', {type: 'success', message: this.$t('alert.success.task.completed')});
          } else {
            this.$root.$emit('show-alert', {type: 'success', message: this.$t('alert.success.task.unCompleted')});
          }
          this.$emit('update-task-completed', updatedTask);
          this.task.task.completed = task.isCompleted;
        }).catch(e => {
          console.error('Error updating project', e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t('alert.error')
          });
          this.postProject = false;
        });
      }


    },
    getTitleTaskClass() {
      if (this.task.task.completed === true) {
        return 'text-color strikethrough';
      } else {
        return 'text-color';
      }
    },
    dateFormatter(dueDate) {
      if (dueDate) {
        const date = new Date(dueDate);
        const day = date.getDate();
        const month = date.getMonth()+1;
        const year = date.getFullYear();
        return `${year}-${month}-${day}`;
      }
    },
    getOverdueTask(value){
      const Today = new Date();
      const formattedTimeToday = `${  Today.getFullYear()}-${  Today.getMonth()+1  }-${Today.getDate()  }`;
      const date = this.dateFormatter(value);
      if (date===formattedTimeToday){
        return false;
      }
      else if (new Date(value) < new Date().getTime()){
        return true;
      } else {
        return false;
      }
    },
    getClassLabels(){
      if (this.task && this.task.labels && this.task.labels.length === 1){
        if (this.assigneeAndCoworkerArray && this.assigneeAndCoworkerArray.length && this.task.commentCount){
          return 'taskLabelsAssigneeComment';
        } else if ((this.assigneeAndCoworkerArray && this.assigneeAndCoworkerArray.length && !this.task.commentCount)
            || (!this.assigneeAndCoworkerArray && !this.assigneeAndCoworkerArray.length && this.task.commentCount)){
          return 'taskLabelsAssignee';
        } else {
          return 'taskLabels';
        }
      }
    },
  }
};
</script>
