import {session, nav, safePath} from './js/core.js';
import LoginRegister from './js/pages/LoginRegister.js';
import Home from './js/pages/Home.js';
import ProductDetail from './js/pages/ProductDetail.js';
import Cart from './js/pages/Cart.js';
import UserCenter from './js/pages/UserCenter.js';
import OrderDetail from './js/pages/OrderDetail.js';
import Shop from './js/pages/Shop.js';
import MerchantProducts from './js/pages/MerchantProducts.js';
import PublishProduct from './js/pages/PublishProduct.js';
import AdminAudit from './js/pages/AdminAudit.js';
import AdminUsers from './js/pages/AdminUsers.js';

const {createApp} = window.Vue;
const routes = [
    [/^\/login$/, LoginRegister],
    [/^\/$|^\/home$/, Home],
    [/^\/product\/\d+$/, ProductDetail],
    [/^\/cart$/, Cart],
    [/^\/orders$/, OrderDetail],
    [/^\/user$/, UserCenter],
    [/^\/shop\/\d+$/, Shop],
    [/^\/merchant\/products$/, MerchantProducts],
    [/^\/merchant\/publish$/, PublishProduct],
    [/^\/admin\/audit$/, AdminAudit],
    [/^\/admin\/users$/, AdminUsers]
];

createApp({
    data() {
        return {path: safePath(location.pathname), user: session.current};
    },
    computed: {
        view() {
            return (routes.find(([pattern]) => pattern.test(this.path)) || routes[1])[1];
        },
        showTopbar() {
            return this.view !== LoginRegister;
        },
        canAdmin() {
            return this.user?.role === 'ADMIN';
        },
        canMerchant() {
            return this.user?.role === 'MERCHANT';
        }
    },
    mounted() {
        if (location.pathname !== this.path) history.replaceState({}, '', this.path);
        window.addEventListener('popstate', () => {
            this.user = session.current;
            const nextPath = safePath(location.pathname);
            if (nextPath !== location.pathname) history.replaceState({}, '', nextPath);
            this.path = nextPath;
        });
    },
    methods: {
        nav(path) {
            nav(safePath(path));
        },
        logout() {
            session.clear();
            this.user = null;
            nav('/login');
        }
    },
    template: `
    <div class="app-shell">
      <header v-if="showTopbar" class="topbar">
        <div class="brand" @click="nav('/home')">校园闲置交易平台</div>
        <nav class="nav">
          <a :class="{active:path==='/home'}" @click.prevent="nav('/home')" href="/home">首页</a>
          <a :class="{active:path==='/cart'}" @click.prevent="nav('/cart')" href="/cart">购物车</a>
          <a :class="{active:path==='/orders'}" @click.prevent="nav('/orders')" href="/orders">订单详情</a>
          <a :class="{active:path==='/user'}" @click.prevent="nav('/user')" href="/user">个人中心</a>
          <a v-if="canMerchant" :class="{active:path==='/merchant/products'}" @click.prevent="nav('/merchant/products')" href="/merchant/products">商家工作台</a>
          <a v-if="canAdmin" :class="{active:path==='/admin/audit'}" @click.prevent="nav('/admin/audit')" href="/admin/audit">审核管理</a>
          <a v-if="canAdmin" :class="{active:path==='/admin/users'}" @click.prevent="nav('/admin/users')" href="/admin/users">用户管理</a>
          <a @click.prevent="logout" href="/login">退出登录</a>
        </nav>
      </header>
      <component :is="view"></component>
    </div>`
}).mount('#app');
