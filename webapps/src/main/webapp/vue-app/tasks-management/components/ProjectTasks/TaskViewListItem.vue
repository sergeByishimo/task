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
  <div
    :class="[getTaskPriorityColor(task.task.priority), removeCompletedTask && 'completedTask' || '']"
    class="taskListItemView px-4 py-3 d-flex align-center">
    <div class="taskCheckBox">
      <v-switch
        ref="autoFocusInput2"
        class="d-none"
        true-value="true"
        false-value="false" />
      <i 
        :title="$t(getTaskCompletedTitle())" 
        :class="getTaskCompleted()" 
        @click="updateCompleted"></i>
    </div>
    <div class="taskTitle pe-10">
      <a
        ref="tooltip"
        :class="getTitleTaskClass()"
        class="text-truncate"
        :title="task.task.title"
        @click="openTaskDrawer()">{{ task.task.title }}
      </a>
    </div>
    <div class="taskAssignee d-flex v-avatar flex-nowrap me-2 justify-start">
      <exo-user-avatars-list
        :users="avatarToDisplay"
        :max="1"
        :icon-size="26"
        avatar-overlay-position
        retrieve-extra-information
        @open-detail="$root.$emit('displayTasksAssigneeAndCoworker', avatarToDisplay)" />
    </div>
    <div class="taskLabels pe-7" @click="openTaskDrawer()">
      <v-chip
        v-if="task.labels && task.labels.length == 1"
        :color="task.labels[0].color"
        :title="task.labels[0].name"
        class="mx-1 font-weight-bold"
        label
        small>
        <span class="text-truncate">
          {{ task.labels[0].name }}
        </span>
      </v-chip>
      <div
        v-else-if="task.labels && task.labels.length > 1"
        :title="getLabelsList(task.labels)"
        class="taskTags d-flex align-center justify-center">
        <i class="uiIcon uiTagIcon me-1"></i>
        <span class="taskAttachNumber caption">{{ task.labels.length }}</span>
      </div>
    </div>
    <div class="taskActions d-flex justify-center align-center pe-9" @click="openTaskDrawer()">
      <div v-if="task.commentCount" class="taskComment d-flex align-center">
        <i class="far fa-comment uiCommentIcon"></i>
        <span class="taskCommentNumber caption">{{ task.commentCount }}</span>
      </div>
    </div>
    <div class="taskStat d-lg-block d-md-none pe-9" @click="openTaskDrawer()">
      <span
        v-if="task && task.task && task.task.status && task.task.status"
        :title="getTaskStatusLabel(task.task.status.name)"
        class="taskStatLabel ps-2">
        {{ getTaskStatusLabel(task.task.status.name) }}
      </span>
    </div>
    <div class="taskDueDate" @click="openTaskDrawer()">
      <div v-if="taskDueDate" :class="getOverdueTask(taskDueDate) ? 'red--text' : ''">
        <date-format :value="taskDueDate" :format="dateTimeFormat" />
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    task: {
      type: Object,
      default: null
    },
    showCompletedTasks: {
      type: Boolean,
      default: false
    }
  },
  data () {
    return {
      enabled: false,
      dateTimeFormat: {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
      },
      assigneeAndCoworkerArray: [],
      isPersonnalTask: this.task.task.status === null,
      labelList: '',
    };
  },
  computed: {
    taskDueDate() {
      return this.task && this.task.task.dueDate && this.task.task.dueDate.time;
    },
    avatarToDisplay () {
      const usersList = [];
      if (this.assigneeAndCoworkerArray && this.assigneeAndCoworkerArray.length ) {
        this.assigneeAndCoworkerArray.forEach(user => {
          usersList.push({'userName': user.username});
        });
      }
      return usersList;
    },
    removeCompletedTask() {
      return this.task.task.completed === true && !this.showCompletedTasks;
    }
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
    this.getTaskAssigneeAndCoworkers();
    this.$root.$on('update-completed-task',(value,id)=>{
      if (this.task.id === id){
        this.task.task.completed=value;
      }
    });
  },
  methods: {
    getTaskPriorityColor(priority) {
      switch (priority) {
      case 'HIGH':
        return 'taskHighPriority';
      case 'NORMAL':
        return 'taskNormalPriority';
      case 'LOW':
        return 'taskLowPriority';
      case 'NONE':
        return 'taskNonePriority';
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
    openTaskDrawer() {
      this.$root.$emit('open-task-drawer', this.task.task);
    },
    getTaskCompleted() {
      if (this.task.task.completed===true){
        return 'uiIconValidate';
      }
      else {
        return 'uiIconCircle';
      }
    },
    getTaskCompletedTitle() {
      if (this.task.task.completed===true){
        return 'message.markAsUnCompleted';
      }
      else {
        return 'message.markAsCompleted';
      }
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
      if (this.task.task.completed===true){
        return 'text-color strikethrough';
      }
      else {
        return 'text-color';
      }
    },
    getTaskStatusLabel(status) {
      switch (status) {
      case 'ToDo':
        return this.$t('exo.tasks.status.todo');
      case 'InProgress':
        return this.$t('exo.tasks.status.inprogress');
      case 'WaitingOn':
        return this.$t('exo.tasks.status.waitingon');
      case 'Done':
        return this.$t('exo.tasks.status.done');
      default:
        return status;
      }
    },
    dateFormatter(dueDate) {
      if (dueDate) {
        const date = new Date(dueDate);
        const day = date.getDate();
        const month = date.getMonth()+1;
        const year = date.getFullYear();
        const formattedTime = `${  year}-${  month  }-${day  }`;
        return formattedTime;
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
  }
};
</script>
