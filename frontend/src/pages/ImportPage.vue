<template>
 <el-card shadow="never"><template #header><strong>导入与校对</strong></template>
 <el-alert :title="aiConfigured ? '文本可由 DeepSeek 提取建议；图片和录音暂需人工填写。所有来源都要校对确认后才会创建任务。' : '当前为人工校对模式，上传后请填写任务信息再确认保存。'" type="info" :closable="false" />
 <el-row :gutter="24" class="content"><el-col :xs="24" :md="10">
  <el-form label-position="top"><el-form-item label="来源类型"><el-select v-model="sourceType" :disabled="busy"><el-option label="文本文件" value="TEXT"/><el-option label="截图或图片" value="SCREENSHOT"/><el-option label="录音文件" value="AUDIO"/></el-select></el-form-item>
  <el-form-item label="选择文件"><input type="file" :accept="sourceType==='TEXT'?'.txt':sourceType==='SCREENSHOT'?'image/*':'audio/*'" @change="selectFile" aria-label="选择导入文件" /></el-form-item>
  <el-button type="primary" :disabled="!file || busy || job?.status==='COMMITTED'" :loading="busy" @click="upload">{{job?.status==='FAILED'&&!job?.proposal?'重试识别':'上传并生成提案'}}</el-button>
  </el-form>
 </el-col><el-col :xs="24" :md="14">
  <el-empty v-if="!job" description="上传后在这里校对" />
  <el-result v-else-if="job.status==='COMMITTED'" icon="success" title="任务已保存"><template #extra><el-button @click="$router.push('/tasks')">查看任务</el-button></template></el-result>
  <template v-else><el-tag>{{states[job.status]??job.status}}</el-tag><el-alert v-if="job.errors.length" :title="job.errors.map(e=>e.message).join('；')" type="error" :closable="false" />
   <el-form v-if="job.proposal" label-position="top" class="content" @submit.prevent="confirm">
    <el-alert v-if="job.proposal.ambiguities.length" :title="job.proposal.ambiguities.join('；')" type="warning" :closable="false" />
    <el-form-item label="任务名称"><el-input v-model="form.title" :disabled="job.status==='FAILED'" maxlength="100" /></el-form-item>
    <el-form-item label="截止时间（北京时间）"><el-input v-model="form.deadline" :disabled="job.status==='FAILED'" type="datetime-local" /></el-form-item>
    <el-form-item label="预计耗时（分钟）"><el-input-number v-model="form.durationMinutes" :disabled="job.status==='FAILED'" :min="1" :max="10080" /></el-form-item>
    <el-button type="primary" :loading="busy" @click="confirm">{{job.status==='FAILED'?'原样重试确认':'已核对，确认创建任务'}}</el-button>
   </el-form>
  </template>
 </el-col></el-row>
 </el-card>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { http } from '@/api/client'
import { beijing, utc, futureDeadline } from '@/utils/time'
interface Proposal{schemaVersion:number;proposalId:string;kind:string;title:string;deadline:string|null;durationMinutes:number|null;ambiguities:string[];errors:{message:string}[];status:string;[key:string]:unknown}
interface Job{requestKey:string;status:string;proposal:Proposal|null;errors:{message:string}[]}
const sourceType=ref('TEXT'),file=ref<File|null>(null),busy=ref(false),job=ref<Job|null>(null),requestKey=ref(sessionStorage.getItem('teamAssemblyImport')??''),aiConfigured=ref(false)
const form=reactive({title:'',deadline:futureDeadline(),durationMinutes:60})
const states:Record<string,string>={REVIEW:'待校对',FAILED:'失败',COMMITTED:'已保存',PROCESSING:'处理中'}
function accept(value:Job){job.value=value;if(value.proposal){form.title=value.proposal.title;form.deadline=value.proposal.deadline?beijing(value.proposal.deadline).slice(0,16):futureDeadline();form.durationMinutes=value.proposal.durationMinutes??60}}
function selectFile(event:Event){file.value=(event.target as HTMLInputElement).files?.[0]??null;requestKey.value='';job.value=null}
async function upload(){if(!file.value)return;if(file.value.size>10*1024*1024){ElMessage.error('文件不能超过10MB');return}busy.value=true;try{
 if(!requestKey.value)requestKey.value=crypto.randomUUID();sessionStorage.setItem('teamAssemblyImport',requestKey.value)
 const data=new FormData();data.append('file',file.value);data.append('sourceType',sourceType.value)
 accept((await http.post<Job>('/imports',data,{headers:{'X-Request-Key':requestKey.value,'Content-Type':undefined}})).data)
 }catch(e){ElMessage.error((e as Error).message)}finally{busy.value=false}}
async function confirm(){if(!job.value?.proposal)return;busy.value=true;try{
 const proposal=job.value.status==='FAILED'?job.value.proposal:{...job.value.proposal,title:form.title.trim(),deadline:utc(form.deadline),durationMinutes:form.durationMinutes,ambiguities:[],errors:[],status:'REVIEW'}
 accept((await http.post<Job>(`/imports/${encodeURIComponent(requestKey.value)}/confirm`,proposal)).data)
 }catch(e){ElMessage.error((e as Error).message)}finally{busy.value=false}}
onMounted(async()=>{try{aiConfigured.value=(await http.get<{aiConfigured:boolean}>('/health')).data.aiConfigured;if(requestKey.value)accept((await http.get<Job>(`/imports/${encodeURIComponent(requestKey.value)}`)).data)}catch(e){ElMessage.error((e as Error).message)}})
</script>
<style scoped>.content{margin-top:20px}</style>
