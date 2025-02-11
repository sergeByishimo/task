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
  <div>
    <v-container
      pa-0
      fluid
      d-flex>
      <i
        :class="$vuetify.rtl && 'fa-chevron-left' || 'fa-chevron-right'"
        class="fa my-auto me-1 ms-2 disabled--text"></i>
      <v-combobox
        ref="select"
        :filter="filterProjects"
        v-model="projectModel"
        :items="projects"
        :label="projectLabel"
        attach
        class="pt-0 mb-0 inputTaskProjectName taskInputArea"
        solo
        prepend-icon
        @click="$emit('projectsListOpened')"
        @change="deleteProject()">
        <template #selection="{ attrs, item, parent, selected }">
          <v-chip
            v-if="item === Object(item)"
            v-bind="attrs"
            :color="`${item.color} lighten-3`"
            :input-value="selected"
            :title="$t('tooltip.clickToEdit')"
            class="projectName"
            small
            close
            text-color="white"
            @click="$emit('projectsListOpened')"
            @click:close="deleteProject">
            <span 
              class="body-2 text-truncate"
              @click="parent.selectItem(item)">
              {{ item.name }}
            </span>
          </v-chip>
        </template>
        <template #item="{ item }">
          <v-list-item @click="updateTaskProject(item)">
            <v-chip
              :color="`${item.color} lighten-3`"
              dark
              close
              text-color="white"
              small>
              <span class="text-truncate">
                {{ item.name }}
              </span>
            </v-chip>
          </v-list-item>
        </template>
      </v-combobox>
    </v-container>
  </div>
</template>

<script>
export default {
  props: {
    task: {
      type: Object,
      default: () => {
        return {};
      }
    }
  },
  data() {
    return {
      projects: [],
      projectModel: null,
      search: null,
      projectLabel: '',
      menuId: `ProjectMenu${parseInt(Math.random() * 10000)
        .toString()
        .toString()}`,
    };
  },
  watch: {
    projectModel () {
      if (this.$refs.select && this.$refs.select.isMenuActive) {
        setTimeout(() => {
          this.$refs.select.isMenuActive = false;
        }, 50);
      }
    },
    task() {
      if (this.task && this.task.status && this.task.status.project && !this.task.status.name) {
        this.$taskDrawerApi.getDefaultStatusByProjectId(this.task.status.project.id).then((status) => {
          this.task.status = status;
        });
      }
    }
  },
  created() {
    this.getProjects();
    $(document).on('mousedown', () => {
      if (this.$refs.select.isMenuActive) {
        window.setTimeout(() => {
          this.$refs.select.isMenuActive = false;
        }, 200);
      }
    });
    document.addEventListener('closeProjectList',()=> {
      setTimeout(() => {
        if (typeof this.$refs.select !== 'undefined') {
          this.$refs.select.isMenuActive = false;
        }
      }, 100);
    });
    document.addEventListener('loadProjectName', event => {
      if (event && event.detail) {
        const task = event.detail;
        if (task.id!=null && task.status && task.status.project) {
          this.projectModel = this.task.status.project;
          this.projectLabel = this.$t('label.tapProject.name');
        } else if (task.id==null && task.status && task.status.project){
          this.projectModel = this.task.status.project;
          this.projectLabel = this.$t('label.tapProject.name');
        } else {
          this.projectModel = null;
          this.projectLabel = this.$t('label.noProject');
        }
      }
    });
  },
  methods: {
    getProjects() {
      this.$taskDrawerApi.getProjects().then((projects) => {
        this.projects = projects.projects;
      });
    },
    filterProjects(item, queryText) {
      return ( item.name.toLocaleLowerCase().indexOf(queryText.toLocaleLowerCase()) >-1 || item.name.toLocaleLowerCase().indexOf(queryText.toLocaleLowerCase()) > -1 );},
    updateTask() {
      this.$taskDrawerApi.updateTask(this.task.id, this.task)
        .then( () => {
          this.$root.$emit('update-task-widget-list', this.task);
          this.$root.$emit('show-alert', { type: 'success', message: this.$t('alert.success.task.project') });})
        .catch(e => {
          console.error('Error when updating task\'s title', e);
          this.$root.$emit('show-alert',{type: 'error',message: this.$t('alert.error')} );
        });
    },
    updateTaskProject(project) {
      this.$taskDrawerApi.getDefaultStatusByProjectId(project.id).then((status) => {
        this.task.status = status;
        this.updateTask();
        this.projectModel = project;
      }).then( () => {
        this.$root.$emit('update-task-project', this.task);
      });
    },
    deleteProject(event) {
      this.task.status = null;
      this.projectModel = null;
      this.projectLabel = this.$t('label.noProject');
      this.updateTask();
      event.preventDefault();
      event.stopPropagation();
    },
  }
};
</script>