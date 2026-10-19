<template>
  <el-alert
    v-if="visible"
    :title="message"
    type="warning"
    show-icon
    :closable="false"
    class="conflict-banner"
  >
    <template #default>
      <span>错误码：{{ code }}。日程已被其他操作修改，请刷新后重试。</span>
      <el-button
        size="small"
        type="primary"
        class="retry-btn"
        @click="onRefresh"
      >
        刷新并重试
      </el-button>
    </template>
  </el-alert>
</template>

<script setup lang="ts">
defineProps<{
  visible: boolean;
  code: string;
  message: string;
}>();

const emit = defineEmits<{
  (e: "refresh"): void;
}>();

function onRefresh() {
  emit("refresh");
}
</script>

<style scoped>
.conflict-banner {
  margin-bottom: 16px;
}
.retry-btn {
  margin-left: 12px;
}
</style>
