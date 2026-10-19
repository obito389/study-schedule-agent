<template>
  <div class="calendar-view">
    <FullCalendar :options="calendarOptions" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import FullCalendar from '@fullcalendar/vue3'
import dayGridPlugin from '@fullcalendar/daygrid'
import timeGridPlugin from '@fullcalendar/timegrid'
import interactionPlugin from '@fullcalendar/interaction'
import type { ScheduleSnapshot, TimeBlock } from '@/types/contract'
import { TimeBlockKind } from '@/types/contract'

const props = defineProps<{
  snapshot: ScheduleSnapshot | null
}>()

/**
 * 把 ScheduleSnapshot 中的课程固定时间、任务已排时间、锁定块
 * 转换为 FullCalendar events。颜色按 kind 区分：
 *   COURSE    蓝色（固定课程）
 *   TASK      绿色（已排任务）
 *   PERSONAL  灰色（个人/锁定）
 *   UNAVAILABLE 红色（不可用时段）
 */
const events = computed(() => {
  if (!props.snapshot) return []
  const list: Array<{
    id: string
    title: string
    start: string
    end: string
    backgroundColor: string
    borderColor: string
    textColor: string
    extendedProps: { kind: TimeBlockKind; locked: boolean }
  }> = []

  // 课程固定时间
  for (const course of props.snapshot.courses) {
    for (const block of course.meetingBlocks) {
      list.push(toCalendarEvent(block, course.name, '#409eff', '#337ecc', '#fff'))
    }
  }

  // 任务已排时间
  for (const task of props.snapshot.tasks) {
    for (const block of task.scheduledBlocks) {
      list.push(toCalendarEvent(block, task.title, '#67c23a', '#529b2e', '#fff'))
    }
  }

  // 锁定块 / 不可用时段
  for (const block of props.snapshot.lockedBlocks) {
    const color =
      block.kind === TimeBlockKind.UNAVAILABLE
        ? { bg: '#f56c6c', border: '#c45656', text: '#fff' }
        : { bg: '#909399', border: '#73767a', text: '#fff' }
    list.push(toCalendarEvent(block, '不可移动', color.bg, color.border, color.text))
  }

  return list
})

function toCalendarEvent(
  block: TimeBlock,
  title: string,
  bg: string,
  border: string,
  text: string
) {
  return {
    id: block.id,
    title,
    start: block.start,
    end: block.end,
    backgroundColor: bg,
    borderColor: border,
    textColor: text,
    extendedProps: { kind: block.kind, locked: block.locked }
  }
}

const calendarOptions = computed(() => ({
  plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin],
  initialView: 'timeGridWeek',
  headerToolbar: {
    left: 'prev,next today',
    center: 'title',
    right: 'dayGridMonth,timeGridWeek,timeGridDay'
  },
  locale: 'zh-cn',
  initialDate: props.snapshot?.now ?? '2026-09-13',
  firstDay: 1,
  slotMinTime: '07:00:00',
  slotMaxTime: '22:00:00',
  slotDuration: '00:30:00', // 与后端 30 分钟时间槽粒度一致
  allDaySlot: false,
  height: 'auto',
  events: events.value,
  editable: false,
  selectable: false
}))
</script>

<style scoped>
.calendar-view {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
}
</style>
