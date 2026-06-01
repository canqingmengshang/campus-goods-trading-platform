import {api, money, nav, safePath, isMerchant} from '../core.js';

const Home = {
    data() {
        return {products: [], filters: {keyword: '', sort: '', minPrice: '', maxPrice: ''}};
    },
    async mounted() {
        await this.load();
    },
    computed: {
        canMerchant() {
            return isMerchant();
        }
    },

    methods: {
        money,
        nav(path) {
            nav(safePath(path));
        },
        detail(id) {
            nav('/product/' + id);
        },
        async load() {
            const query = new URLSearchParams(Object.fromEntries(Object.entries(this.filters).filter(([, v]) => v !== '')));
            this.products = await api.get('/api/products?' + query);
        }
    },
    template: `
    <main class="container">
      <section class="hero">
        <div class="hero-main">
          <h1>校园闲置交易平台</h1>
          <p>图书、数码、代步工具、宿舍电器统一发布审核，买家可用钱包和积分完成交易。</p>
          <div class="actions">
            <button v-if="canMerchant" @click="nav('/merchant/publish')">发布闲置</button>
            <button class="secondary" @click="nav('/cart')">查看购物车</button>
          </div>
        </div>
        <div class="stat-strip">
          <div class="panel">
            <b>推荐位轮播</b>
            <p class="muted">山地车、小冰箱、考研资料正在热卖。</p>
          </div>
          <div class="panel">
            <b>交易保障</b>
            <p class="muted">商品审核、商家审核、24小时退货申请、五星评价闭环。</p>
          </div>
        </div>
      </section>
      <section class="panel">
        <div class="search-row">
          <input v-model="filters.keyword" placeholder="按商品名称搜索">
          <button @click="load">搜索</button>
        </div>
        <div class="filter-row">
          <select v-model="filters.sort">
            <option value="">默认排序</option>
            <option value="price">价格</option>
            <option value="sales">销量</option>
            <option value="rate">好评率</option>
          </select>
          <input v-model="filters.minPrice" placeholder="最低价">
          <input v-model="filters.maxPrice" placeholder="最高价">
          <button @click="load">筛选</button>
        </div>
      </section>
      <section class="grid" style="margin-top:16px">
        <article v-for="p in products" :key="p.id" class="card">
          <img class="product-img" :src="p.photos[0]" @click="detail(p.id)">
          <div class="card-body">
            <div class="title-row">
              <b>{{p.name}}</b>
              <span class="badge">{{p.condition}}</span>
            </div>
            <div>
              <span class="price">￥{{money(p.salePrice)}}</span>
              <span class="muted"><s>￥{{money(p.originalPrice)}}</s></span>
            </div>
            <p class="muted">{{p.merchantName}} · 销量 {{p.sales}} · 好评 {{p.favorableRate}}%</p>
            <button @click="detail(p.id)">查看详情</button>
          </div>
        </article>
      </section>
    </main>`
};

export default Home;
