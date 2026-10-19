<template>
 <el-dialog v-model="visible" title="课程与固定时段" width="600px">
  <el-form label-position="top">
   <el-form-item label="类型"><el-radio-group v-model="kind"><el-radio-button value="courses">每周课程</el-radio-button><el-radio-button value="windows">当天不可用时段</el-radio-button></el-radio-group></el-form-item>
   <el-form-item v-if="kind==='courses'" label="课程名称"><el-input v-model="form.title" maxlength="100" /></el-form-item>
   <el-form-item :label="kind==='courses'?'首次上课日期':'日期'"><el-input v-model="form.firstDate" type="date" /></el-form-item>
   <div class="times"><el-form-item label="开始时间"><el-input v-model="form.startTime" type="time" /></el-form-item><el-form-item label="结束时间"><el-input v-model="form.endTime" type="time" /></el-form-item></div>
   <el-form-item v-if="kind==='courses'" label="连续上课周数"><el-input-number v-model="form.weeks" :min="1" :max="30" /></el-form-item>
  </el-form>
  <el-button type="primary" :loading="saving" @click="save">确认添加</el-button>
  <el-divider />
  <div v-for="course in config.courses" :key="'c'+course.id" class="entry"><span>{{ course.title }}</span><el-button link type="danger" @click="remove('courses',course.id)">删除</el-button></div>
  <div v-for="window in config.windows" :key="'w'+window.id" class="entry"><span>{{ window.localDate }} {{ window.startTime }}–{{ window.endTime }}</span><el-button link type="danger" @click="remove('windows',window.id)">删除</el-button></div>
 </el-dialog>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/client'
import { getScheduleSnapshot } from '@/api/schedule'
import { beijing } from '@/utils/time'
const props=defineProps<{modelValue:boolean}>()
const emit=defineEmits<{(e:'update:modelValue',v:boolean):void;(e:'changed'):void}>()
const visible=computed({get:()=>props.modelValue,set:v=>emit('update:modelValue',v)})
const kind=ref('courses'),saving=ref(false),base=ref(0)
const form=reactive({title:'',firstDate:beijing(new Date().toISOString()).slice(0,10),startTime:'09:00',endTime:'10:00',weeks:16})
const config=ref<{courses:{id:number;title:string}[];windows:{id:number;localDate:string;startTime:string;endTime:string}[]}>({courses:[],windows:[]})
async function load(){try{const [s,c]=await Promise.all([getScheduleSnapshot(),http.get('/workspace/calendar')]);base.value=s.planningRevision;config.value=c.data}catch(e){ElMessage.error((e as Error).message)}}
watch(visible,v=>{if(v)load()})
async function save(){saving.value=true;try{await http.post('/workspace/calendar/'+kind.value,{...form,baseRevision:base.value});ElMessage.success('已添加');emit('changed');await load()}catch(e){ElMessage.error((e as Error).message);await load()}finally{saving.value=false}}
async function remove(type:string,id:number){try{await ElMessageBox.confirm('确认删除此项？','删除确认');await http.delete(`/workspace/calendar/${type}/${id}`,{params:{baseRevision:base.value}});emit('changed');await load()}catch(e){if(e!=='cancel'&&e!=='close')ElMessage.error((e as Error).message)}}
</script>
<style scoped>.times{display:flex;gap:20px}.entry{display:flex;justify-content:space-between;align-items:center;padding:8px 0}</style>
