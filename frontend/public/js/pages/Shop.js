import {api, session, money, nav} from '../core.js';

const Shop = {
    data() {
        return {products: [], keyword: '', sort: 'sales', msg: ''};
    },
    async mounted() {
        this.products = await api.get('/api/shops/' + location.pathname.split('/').pop() + '/products');
    },
    computed: {
        visibleProducts() {
            const word = this.keyword.trim().toLowerCase();
            const products = this.products
                .filter(product => product.status === 'PUBLISHED')
                .filter(product => !word || product.name.toLowerCase().includes(word) || (product.category || '').toLowerCase().includes(word));
            return [...products].sort((a, b) => {
                if (this.sort === 'price') return Number(a.salePrice) - Number(b.salePrice);
                if (this.sort === 'rate') return Number(b.favorableRate) - Number(a.favorableRate);
                if (this.sort === 'stock') return Number(b.stock) - Number(a.stock);
                return Number(b.sales) - Number(a.sales);
            });
        },
        shopName() {
            return this.products[0]?.merchantName || '商家店铺';
        },
        totalSales() {
            return this.visibleProducts.reduce((sum, product) => sum + Number(product.sales || 0), 0);
        },
        totalStock() {
            return this.visibleProducts.reduce((sum, product) => sum + Number(product.stock || 0), 0);
        },
        avgRate() {
            if (!this.visibleProducts.length) return '100.0';
            const total = this.visibleProducts.reduce((sum, product) => sum + Number(product.favorableRate || 100), 0);
            return (total / this.visibleProducts.length).toFixed(1);
        },
        reviews() {
            return this.products.flatMap(product => (product.reviews || [])
                .map(review => ({...review, productName: product.name})))
                .slice(0, 8);
        }
    },
    methods: {
        money,
        detail(id) {
            nav('/product/' + id);
        },
        async addCart(product, buyNow = false) {
            await api.post('/api/users/' + session.current.id + '/cart/' + product.id + '?quantity=1');
            if (buyNow) {
                nav('/cart');
            } else {
                this.msg = product.name + ' 已加入购物车';
            }
        }
    },
    template: `
    <main class="container">
      <section class="shop-hero panel">
        <div>
          <h2>{{shopName}}</h2>
          <p class="muted">按店铺维度展示该商家的上架商品、库存、历史销量与历史评价。</p>
        </div>
        <div class="shop-stats">
          <div><b>{{visibleProducts.length}}</b><span>上架商品</span></div>
          <div><b>{{totalStock}}</b><span>库存总量</span></div>
          <div><b>{{totalSales}}</b><span>历史销量</span></div>
          <div><b>{{avgRate}}%</b><span>平均好评</span></div>
        </div>
      </section>

      <section class="panel" style="margin-top:16px">
        <div class="search-row">
          <input v-model="keyword" placeholder="搜索本店商品名称或类别">
          <button>搜索</button>
        </div>
        <div class="filter-row">
          <select v-model="sort">
            <option value="sales">销量优先</option>
            <option value="price">价格从低到高</option>
            <option value="rate">好评率优先</option>
            <option value="stock">库存优先</option>
          </select>
        </div>
        <p v-if="msg" class="notice">{{msg}}</p>
      </section>

      <section class="grid" style="margin-top:16px">
        <article v-for="p in visibleProducts" class="card" :key="p.id">
          <img class="product-img" :src="p.photos[0]" @click="detail(p.id)">
          <div class="card-body">
            <div class="title-row">
              <b>{{p.name}}</b>
              <span class="badge">{{p.condition}}</span>
            </div>
            <p class="price">￥{{money(p.salePrice)}}</p>
            <p class="muted">库存 {{p.stock}} · 销量 {{p.sales}} · 好评 {{p.favorableRate}}%</p>
            <p class="muted">{{p.negotiable ? '可议价' : '不议价'}} · {{p.category}}</p>
            <div class="actions">
              <button class="secondary" @click="detail(p.id)">详情</button>
              <button @click="addCart(p)">加购物车</button>
              <button class="danger" @click="addCart(p, true)">购买</button>
            </div>
          </div>
        </article>
      </section>

      <section class="panel" style="margin-top:16px">
        <div class="title-row">
          <h2>历史评价</h2>
          <span class="muted">{{reviews.length ? '最近评价' : '暂无评价'}}</span>
        </div>
        <div v-if="reviews.length" class="review-list">
          <div v-for="review in reviews" :key="review.id" class="review-item">
            <b>{{review.productName}} · {{review.stars}} 星</b>
            <p>{{review.content || '买家未填写文字评价'}}</p>
          </div>
        </div>
        <p v-else class="muted">该店铺暂未收到文字评价。</p>
      </section>
    </main>`
};

export default Shop;
