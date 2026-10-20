import { Layout } from '@/utils/routerHelper'
const remainingRouter: AppRouteRecordRaw[] = [
 {path:'/redirect',component:Layout,name:'RedirectRoot',children:[{path:'/redirect/:path(.*)',name:'Redirect',component:()=>import('@/views/Redirect/Redirect.vue'),meta:{}}],meta:{hidden:true,noTagsView:true}},
 {path:'/',component:Layout,redirect:'/stock/product',name:'Home',meta:{hidden:true},children:[{path:'index',name:'Index',component:()=>import('@/views/Home/Index.vue'),meta:{title:'运营概览',icon:'ep:home-filled',affix:true}}]},
 {path:'/user',component:Layout,name:'User',meta:{hidden:true},children:[{path:'profile',name:'Profile',component:()=>import('@/views/Profile/Index.vue'),meta:{title:'个人中心',hidden:true}},{path:'notify-message',name:'MyNotifyMessage',component:()=>import('@/views/system/notify/my/index.vue'),meta:{title:'站内信',hidden:true}}]},
 {path:'/dict',component:Layout,name:'Dict',meta:{hidden:true},children:[{path:'type/data/:dictType',name:'SystemDictData',component:()=>import('@/views/system/dict/data/index.vue'),meta:{title:'字典数据',hidden:true}}]},
 {path:'/login',component:()=>import('@/views/Login/Login.vue'),name:'Login',meta:{hidden:true,noTagsView:true}},
 {path:'/sso',component:()=>import('@/views/Login/Login.vue'),name:'SSOLogin',meta:{hidden:true,noTagsView:true}},
 {path:'/social-login',component:()=>import('@/views/Login/SocialLogin.vue'),name:'SocialLogin',meta:{hidden:true,noTagsView:true}},
 {path:'/403',component:()=>import('@/views/Error/403.vue'),name:'NoAccess',meta:{hidden:true}},
 {path:'/:path(.*)*',component:()=>import('@/views/Error/404.vue'),name:'NoFound',meta:{hidden:true,noTagsView:true}}
]
export default remainingRouter
