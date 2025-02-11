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
  <v-app id="ProjectCardList" class="tasksListContainer">
    <div
      v-if="(!projects || !projects.length) && !loadingProjects"
      class="noTasksProject">
      <div class="noTasksProjectIcon"><i class="uiIcon uiIconClipboard"></i></div>
      <div class="noTasksProjectLabel"><span>{{ $t('label.noProjects') }}</span></div>
    </div>
    <div v-else>
      <v-card flat>
        <v-item-group class="pa-4">
          <v-container class="pa-0">
            <v-row class="ma-0 border-box-sizing">
              <v-col
                v-for="project in projects"
                :key="project.id"
                :id="'project-'+project.id"
                cols="12"
                md="6"
                lg="4"
                xl="4"
                class="pa-0 projectItem">
                <project-card
                  :project="project"
                  @refreshProjects="searchProjects" />
              </v-col>
            </v-row>
            <v-row class="ma-0 border-box-sizing">
              <v-btn
                v-if="canShowMore"
                :loading="loadingProjects"
                :disabled="loadingProjects"
                class="loadMoreButton ma-auto mt-4 btn"
                block
                @click="loadNextPage">
                {{ $t('spacesList.button.showMore') }}
              </v-btn>
            </v-row>
          </v-container>
        </v-item-group>
      </v-card>
    </div>
  </v-app>
</template>
<script>
export default {
  props: {
    spaceName: {
      type: String,
      default: '',
    },
    keyword: {
      type: String,
      default: null,
    },
    projectFilterSelected: {
      type: String,
      default: 'ALL',
    },
    loadingProjects: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      maxTasksSize: 10,
      projects: [],
      offset: 0,
      projectSize: 0,
      pageSize: 20,
      limit: 20,
      limitToFetch: 0,
      originalLimitToFetch: 0,
      disableBtn: false,
      startSearchAfterInMilliseconds: 600,
      endTypingKeywordTimeout: 50,
      startTypingKeywordTimeout: 0,
    };
  },
  computed: {
    canShowMore() {
      return this.loadingProjects || this.projects.length >= this.limitToFetch;
    },
  },
  watch: {
    keyword() {
      if (!this.keyword) {
        this.resetSearch();
        this.searchProjects();
        return;
      }
      this.startTypingKeywordTimeout = Date.now();
      if (!this.loadingProjects) {
        this.loadingProjects = true;
        this.waitForEndTyping();
      }
    },
    limitToFetch() {
      this.searchProjects();
    },
    projectFilterSelected() {
      this.searchProjects();
    },

  },
  created() {
    this.originalLimitToFetch = this.limitToFetch = this.limit;
    this.$root.$on('update-projects-list', () => {
      this.searchProjects();
    });
  },
  methods: {
    searchProjects() {
      this.loadingProjects = true;
      return this.$projectService.getProjectsList(this.spaceName,this.keyword,this.projectFilterSelected,this.offset, this.limitToFetch).then(data => {
        //this.projects.push(...data.projects);
        this.projects = data && data.projects || [];
        this.projectSize = data && data.projectNumber || 0;
        return this.$nextTick();
      }).then(() => {
        if (this.keyword && this.projects.length >= this.limitToFetch) {
          this.limitToFetch += this.pageSize;
        }
      })
        .finally(() => {
          this.loadingProjects = false;
          this.$root.$applicationLoaded();
        });
    },
    resetSearch() {
      if (this.limitToFetch !== this.originalLimitToFetch) {
        this.limitToFetch = this.originalLimitToFetch;
      }
    },
    loadNextPage() {
      this.originalLimitToFetch = this.limitToFetch += this.pageSize;
    },
    waitForEndTyping() {
      window.setTimeout(() => {
        if (Date.now() - this.startTypingKeywordTimeout > this.startSearchAfterInMilliseconds) {
          this.searchProjects();
        } else {
          this.waitForEndTyping();
        }
      }, this.endTypingKeywordTimeout);
    },
  }
};
</script>
