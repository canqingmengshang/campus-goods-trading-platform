import {api, session, money, nav} from '../core.js';

const ProductDetail = {
    data() {
        return {product: null, quantity: 1, msg: ''};
    },
    async mounted() {
        this.product = await api.get('/api/products/' + location.pathname.split('/').pop());
    },
    methods: {
        nav,
        money,
        async addCart(buyNow) {
            await api.post('/api/users/' + session.current.id + '/cart/' + this.product.id + '?quantity=' + this.quantity);
            buyNow ? nav('/cart') : this.msg = '已加入购物车';
        }
    },
    template: `
    <main class="container" v-if="product">
      <section class="split">
        <div class="panel detail-media">
          <img class="product-img" :src="product.photos[0]">
        </div>
        <div class="panel">
          <h2>{{product.name}}</h2>
          <p>
            <span class="price">￥{{money(product.salePrice)}}</span>
            <span class="muted">原价 <s>￥{{money(product.originalPrice)}}</s></span>
          </p>
          <p>
            <span class="badge">{{product.condition}}</span>
            <span class="badge">{{product.negotiable ? '可议价' : '不议价'}}</span>
          </p>
          <p class="muted">尺寸：{{product.size}} · 库存：{{product.stock}} · 历史销量：{{product.sales}}</p>
          <p>{{product.usageGuide}}</p>
          <div class="panel">
            <b>{{product.merchantName}}</b>
            <p class="muted">商品好评率 {{product.favorableRate}}%，支持查看商家历史评价。</p>
            <button class="secondary" @click="nav('/shop/' + product.merchantId)">进入店铺</button>
          </div>
          <label>数量<input type="number" min="1" v-model.number="quantity"></label>
          <div class="actions">
            <button @click="addCart(false)">加入购物车</button>
            <button class="danger" @click="addCart(true)">立即购买</button>
          </div>
          <p v-if="msg" class="notice">{{msg}}</p>
        </div>
      </section>
    </main>`
};

export default ProductDetail;
