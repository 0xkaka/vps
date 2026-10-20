<template>
  <div class="app-container">
    <h2>{{ title }}</h2>
    <el-alert v-if="resource==='product'" title="优先读取商家官方接口或购买页；失败时使用备用来源。实际来源和异常原因见表格。新增产品需提供正确商家购买编号；关闭同步可手动维护。" type="info" :closable="false" style="margin-bottom:18px" />
    <el-alert v-if="resource==='subscriber'" title="邮件服务尚需部署配置 SMTP；此页面管理订阅记录，实际投递状态以邮件服务配置为准。" type="warning" :closable="false" style="margin-bottom:18px" />
    <el-form inline @submit.prevent="search"><el-form-item label="名称"><el-input v-model="query.name" clearable @keyup.enter="search" /></el-form-item><el-button @click="search">查询</el-button>
      <el-button v-if="!readonly" type="primary" v-hasPermi="['stock:'+resource+':edit']" @click="edit()">新增</el-button>
      <el-button v-if="resource==='product'" v-hasPermi="['stock:product:edit']" :loading="syncing" @click="sync">同步库存</el-button>
    </el-form>
    <el-table v-loading="loading" :data="rows" border stripe>
      <el-table-column prop="id" label="ID" width="75" />
      <el-table-column v-for="f in visibleFields" :key="f.key" :prop="f.key" :label="f.label" :min-width="f.key==='name'?230:f.key==='lastCheckedAt'?180:f.type==='bool'?90:120" show-overflow-tooltip>
        <template #default="{row}"><span>{{ display(f,row[f.key]) }}</span></template>
      </el-table-column>
      <el-table-column v-if="resource==='product'" label="实际数据来源" min-width="180"><template #default="{row}"><span :title="row.lastSourceUrl">{{ {OFFICIAL_API:'商家官方 API',OFFICIAL_PAGE:'商家官方页面',THIRD_PARTY_PAGE:'备用聚合来源',THIRD_PARTY_API:'星空VPS · 第三方 API'}[row.lastSourceType]||'尚未同步' }}</span></template></el-table-column>
      <el-table-column v-if="resource==='product'" prop="lastSyncError" label="同步异常 / 备用原因" min-width="260" show-overflow-tooltip />
      <el-table-column v-if="!readonly" label="操作" fixed="right" width="145"><template #default="{row}"><el-button link type="primary" v-hasPermi="['stock:'+resource+':edit']" @click="edit(row)">编辑</el-button><el-button link type="danger" v-hasPermi="['stock:'+resource+':edit']" @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="query.pageNo" v-model:limit="query.pageSize" @pagination="load" />
    <el-dialog v-model="opened" :title="(form.id?'编辑':'新增')+title" width="min(760px,95vw)" top="5vh" destroy-on-close>
      <el-form ref="formRef" :model="form" label-width="150px" style="max-height:65vh;overflow:auto;padding-right:15px">
        <el-form-item v-for="f in fields" :key="f.key" :label="f.label" :prop="f.key" :rules="f.required?[{required:true,message:'请填写'+f.label,trigger:'blur'}]:[]">
          <el-switch v-if="f.type==='bool'" v-model="form[f.key]" />
          <el-select v-else-if="options[f.key]" v-model="form[f.key]" clearable filterable style="width:100%"><el-option v-for="o in options[f.key]" :key="o.value" :label="o.label" :value="o.value" /></el-select>
          <el-input-number v-else-if="f.type==='number'" v-model="form[f.key]" :min="f.key==='intervalSeconds'?120:0" :precision="0" />
          <el-date-picker v-else-if="f.type==='datetime'" v-model="form[f.key]" type="datetime" value-format="x" />
          <el-input v-else v-model="form[f.key]" :maxlength="f.max||2000" :type="f.key==='description'||f.key==='value'?'textarea':'text'" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="opened=false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import request from '@/config/axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ref, reactive, computed, onMounted } from 'vue'
const props=defineProps({title:String,resource:String,fields:Array,readonly:Boolean})
const rows=ref([]),total=ref(0),loading=ref(false),saving=ref(false),syncing=ref(false),opened=ref(false),formRef=ref(),form=ref({})
const query=reactive({name:'',pageNo:1,pageSize:20})
const options=reactive({sourcePage:[{value:"https://stock.vpszdm.com/",label:"综合库存首页"},{value:"https://stock.bwh91.com/",label:"搬瓦工独立库存"},{value:"https://stock.dmitea.com/",label:"DMIT独立库存"},{value:"https://matrix.bwh91.com/",label:"搬瓦工产品大全"},{value:"https://matrix.dmitea.com/",label:"DMIT产品大全"}],stockStatus:[{value:'IN_STOCK',label:'有货'},{value:'OUT_OF_STOCK',label:'无货'},{value:'UNKNOWN',label:'未知'}],type:['TELEGRAM','BARK','NTFY','WXPUSHER','WECHAT','DISCORD','OTHER'].map(value=>({value,label:value})),position:['HEADER','PRODUCT','FOOTER'].map(value=>({value,label:value})),status:['PENDING','ACTIVE','UNSUBSCRIBED'].map(value=>({value,label:value}))})
const visibleFields=computed(()=>props.fields.filter(f=>props.resource==='product'?['name','vendorId','stockStatus','intervalSeconds','lastCheckedAt','monitorEnabled','enabled'].includes(f.key):!['sourceName'].includes(f.key)))
function display(f,v){ if(f.type==='datetime'&&v)return Array.isArray(v)?v.join('-'):new Date(v).toLocaleString('zh-CN',{hour12:false}); if(f.type==='bool')return v?'是':'否'; return options[f.key]?.find(o=>o.value===v)?.label ?? v ?? '—' }
async function load(){ loading.value=true;try{const r=await request.get({url:'/stock/'+props.resource+'/page',params:query});rows.value=r.list;total.value=r.total}finally{loading.value=false} }
function search(){query.pageNo=1;load()}
async function edit(row){
  form.value=row?{...row}:Object.fromEntries(props.fields.map(f=>[f.key,f.key==='sourcePage'?'https://stock.vpszdm.com/':f.type==='bool'?['enabled'].includes(f.key):f.key==='intervalSeconds'?120:f.key==='sortOrder'?0:f.type==='number'?null:'']))
  if(!row&&props.resource==='product')form.value.stockStatus='UNKNOWN'
  for(const [field,resource] of [['vendorId','vendor'],['parentId','product'],['productId','product']]){
    if(props.fields.some(f=>f.key===field)){const r=await request.get({url:'/stock/'+resource+'/page',params:{pageNo:1,pageSize:1000}});options[field]=r.list.filter(p=>field!=='parentId'||p.groupFlag&&p.id!==row?.id).map(p=>({value:p.id,label:p.name+' (#'+p.id+')'}))}
  }
  opened.value=true
}
async function save(){await formRef.value.validate(); for(const f of props.fields){if(['parentId','productId','vendorId'].includes(f.key)&&form.value[f.key]==='')form.value[f.key]=null;}saving.value=true;try{await request.post({url:'/stock/'+props.resource+'/save',data:form.value});opened.value=false;ElMessage.success('保存成功');await load()}finally{saving.value=false}}
async function remove(row){try{await ElMessageBox.confirm('确认删除 '+(row.name||row.id)+'？有关联产品的商家或分组需先处理关联数据。','删除确认',{type:'warning'})}catch{return}await request.delete({url:'/stock/'+props.resource+'/delete',params:{id:row.id}});ElMessage.success('删除成功');load()}
async function sync(){syncing.value=true;try{const r=await request.post({url:'/stock/sync',timeout:180000});ElMessage.success('已同步 '+r+' 条记录');load()}finally{syncing.value=false}}
async function loadReferences(){for(const [field,resource] of [['vendorId','vendor'],['productId','product']]){if(props.fields.some(f=>f.key===field)){try{const r=await request.get({url:'/stock/'+resource+'/page',params:{pageNo:1,pageSize:1000}});options[field]=r.list.map(p=>({value:p.id,label:p.name+' (#'+p.id+')'}))}catch{ /* 主表仍可加载，引用查询遵循独立权限。 */ }}}}
onMounted(()=>{load();loadReferences()})
</script>
