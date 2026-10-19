<template>
  <el-tag :type="tagType" size="small">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from "vue";
import {
  ACTION_STATUS_LABEL,
  ACTION_STATUS_TAG_TYPE,
} from "../../types/contract";

const props = defineProps<{ status: string }>();

const label = computed(
  () =>
    ACTION_STATUS_LABEL[props.status as keyof typeof ACTION_STATUS_LABEL] ??
    props.status,
);
const tagType = computed(
  () =>
    ACTION_STATUS_TAG_TYPE[
      props.status as keyof typeof ACTION_STATUS_TAG_TYPE
    ] ?? "info",
);
</script>
