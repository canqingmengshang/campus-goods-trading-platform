import {api, session, money, nav, safePath} from '../core.js';

const MerchantProducts = {
    data() {
        return {products: []};
    },
    async mounted() {
        await this.load();
    },
    methods: {
        money,
        nav(path) {
            nav(safePath(path));
        },
        async load() {
            this.products = await api.get('/api/shops/' + session.current.id + '/products');
        },
        async offShelf(id) {
            await api.post('/api/products/' + id + '/status?status=OFF_SHELF');
            await this.load();
        }
    },
    template: `
    <main class="container">
      <section class="panel">
        <div class="title-row">
          <h2>商家工作台</h2>
          <button @click="nav('/merchant/publish')">发布商品</button>
        </div>
        <table class="table">
          <thead>
            <tr>
              <th>商品</th>
              <th>价格</th>
              <th>库存</th>
              <th>销量</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in products" :key="p.id">
              <td>{{p.name}}</td>
              <td>￥{{money(p.salePrice)}}</td>
              <td>{{p.stock}}</td>
              <td>{{p.sales}}</td>
              <td><span class="badge">{{p.status}}</span></td>
              <td><button class="secondary" @click="offShelf(p.id)">下架</button></td>
            </tr>
          </tbody>
        </table>
      </section>
    </main>`
};

export default MerchantProducts;
