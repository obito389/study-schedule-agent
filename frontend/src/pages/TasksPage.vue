<template>
 <el-card shadow="never"><template #header><div class="toolbar"><strong>任务管理</strong><div><el-button @click="load">刷新</el-button><el-button type="primary" :disabled="loading || !snapshot" @click="edit()">新建任务</el-button></div></div></template>
 <el-table :data="snapshot?.tasks ?? []" v-loading="loading" border>
  <el-table-column prop="title" label="任务" min-width="180" />
  <el-table-column label="截止时间（北京时间）" min-width="180"><template #default="{row}">{{ beijing(row.deadline).replace('T',' ') }}</template></el-table-column>
  <el-table-column prop="estimatedDurationMinutes" label="预计分钟" width="100" />
  <el-table-column label="状态" width="110"><template #default="{row}">{{ row.status === 'TODO' && row.scheduledBlocks.length ? '已安排' : labels[row.status] }}</template></el-table-column>
  <el-table-column label="操作" min-width="230"><template #default="{row}"><template v-if="row.status !== 'DONE' && row.status !== 'CANCELLED'"><el-button link type="primary" @click="edit(row)">编辑</el-button><el-button link type="success" @click="complete(row)">完成并反馈</el-button><el-button link type="danger" @click="cancel(row)">取消</el-button></template></template></el-table-column>
 </el-table>
 <el-empty v-if="!loading && !snapshot?.tasks.length" description="还没有任务，请先新建或从导入页面添加" />
 <el-alert title="创建任务后，可在日程日历中生成规则排程，核对预览后确认保存。" type="info" :closable="false" class="hint" />
 </el-card>
 <el-dialog v-model="open" :title="editingId ? '编辑任务' : '新建任务'" width="500px" :close-on-click-modal="false">
  <el-form label-position="top" @submit.prevent="save">
   <el-form-item label="任务名称"><el-input v-model="form.title" maxlength="100" /></el-form-item>
   <el-form-item label="截止时间（北京时间）"><el-input v-model="form.deadline" type="datetime-local" /></el-form-item>
   <el-form-item label="预计剩余耗时（分钟）"><el-input-number v-model="form.durationMinutes" :min="1" :max="10080" :step="30" /></el-form-item>
   <el-form-item label="优先级（数值越大越优先）"><el-input-number v-model="form.priority" :min="1" :max="3" /></el-form-item>
  </el-form>
  <template #footer><el-button @click="open=false">返回</el-button><el-button type="primary" :loading="saving" @click="save">确认保存任务</el-button></template>
 </el-dialog>
</template>
<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { http } from '@/api/client'
import { getScheduleSnapshot } from '@/api/schedule'
import type { ScheduleSnapshot, TaskSnapshot } from '@/types/contract'
import { beijing, utc, futureDeadline } from '@/utils/time'
const snapshot=ref<ScheduleSnapshot|null>(null),loading=ref(false),saving=ref(false),open=ref(false),editingId=ref<string|null>(null)
const baseRevision=ref(0), operationKey=ref('')
const form=reactive({title:'',deadline:futureDeadline(),durationMinutes:60,priority:2})
const labels:Record<string,string>={TODO:'待安排',IN_PROGRESS:'进行中',DONE:'已完成',CANCELLED:'已取消'}
async function load(){loading.value=true;try{snapshot.value=await getScheduleSnapshot()}catch(e){ElMessage.error((e as Error).message)}finally{loading.value=false}}
function edit(task?:TaskSnapshot){editingId.value=task?.id ?? null;form.title=task?.title??'';form.deadline=task?beijing(task.deadline).slice(0,16):futureDeadline();form.durationMinutes=task?.estimatedDurationMinutes??60;form.priority=task?.priority??2;baseRevision.value=snapshot.value?.planningRevision??0;operationKey.value=crypto.randomUUID();open.value=true}
async function save(){
 if(!form.title.trim()||!form.deadline){ElMessage.warning('请填写名称和截止时间');return}
 saving.value=true
 try{const body={...form,title:form.title.trim(),deadline:utc(form.deadline),operationKey:operationKey.value,baseRevision:baseRevision.value,courseId:null};
  const response=editingId.value?await http.put<ScheduleSnapshot>(`/workspace/tasks/${editingId.value}`,body):await http.post<ScheduleSnapshot>('/workspace/tasks',body)
  snapshot.value=response.data;open.value=false;ElMessage.success('任务已保存')
 }catch(e){ElMessage.error((e as Error).message);await load()}finally{saving.value=false}
}
async function complete(task:TaskSnapshot){try{
 const {value}=await ElMessageBox.prompt('请输入本次任务实际耗时（分钟）','完成任务与反馈',{inputValue:String(task.estimatedDurationMinutes),inputPattern:/^[1-9][0-9]{0,4}$/,inputErrorMessage:'请输入正整数',confirmButtonText:'确认完成',cancelButtonText:'返回'})
 snapshot.value=(await http.post<ScheduleSnapshot>(`/workspace/tasks/${task.id}/complete`,{operationKey:crypto.randomUUID(),baseRevision:snapshot.value!.planningRevision,actualMinutes:Number(value)})).data;ElMessage.success('已完成，并记录实际耗时')
 }catch(e){if(e!=='cancel'&&e!=='close')ElMessage.error((e as Error).message)}
}
async function cancel(task:TaskSnapshot){try{
 await ElMessageBox.confirm('取消后将释放该任务的已排时间。','确认取消任务',{confirmButtonText:'确认取消',cancelButtonText:'返回'})
 snapshot.value=(await http.post<ScheduleSnapshot>(`/workspace/tasks/${task.id}/cancel`,{operationKey:crypto.randomUUID(),baseRevision:snapshot.value!.planningRevision,actualMinutes:1})).data;ElMessage.success('已取消')
 }catch(e){if(e!=='cancel'&&e!=='close')ElMessage.error((e as Error).message)}
}
onMounted(load)
</script>
<style scoped>.toolbar{display:flex;justify-content:space-between;align-items:center}.hint{margin-top:18px}</style>
