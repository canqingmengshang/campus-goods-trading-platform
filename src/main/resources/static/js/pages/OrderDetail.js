import {api, session, money} from '../core.js';

const OrderDetail = {
    data() {
        return {orders: [], products: {}, review: {stars: 5, content: ''}, msg: ''};
    },
    async mounted() {
        await this.load();
    },
    methods: {
        money,
        async load() {
            this.orders = await api.get('/api/users/' + session.current.id + '/orders');
            for (const order of this.orders) {
                for (const item of order.items) {
                    this.products[item.productId] = await api.get('/api/products/' + item.productId);
                }
            }
        },
        async received(order) {
            await api.post('/api/orders/' + order.id + '/received');
            await this.load();
        },
        async requestReturn(order) {
            await api.post('/api/orders/' + order.id + '/return?reason=' + encodeURIComponent('24小时内退货申请'));
            await this.load();
        },
        async submitReview(order, product) {
            await api.post('/api/users/' + session.current.id + '/reviews', {
                orderId: order.id,
                merchantId: product.merchantId,
                productId: product.id,
                stars: this.review.stars,
                content: this.review.content
            });
            this.msg = '评价已提交';
            await this.load();
        }
    },
    template: `
    <main class="container">
      <section class="panel">
        <h2>订单详情</h2>
        <div v-for="order in orders" :key="order.id" class="panel" style="margin:12px 0">
          <div class="title-row">
            <b>订单 {{order.id}}</b>
            <span class="badge">{{order.status}}</span>
          </div>
          <p>实付 ￥{{money(order.totalAmount)}}，积分抵扣 {{order.pointsUsed}}</p>
          <div v-for="item in order.items" :key="item.productId">
            {{products[item.productId]?.name}} x {{item.quantity}}
            <div class="actions">
              <button class="secondary" v-if="order.status==='PAID'" @click="received(order)">确认收货</button>
              <button class="danger" v-if="order.status==='RECEIVED'" @click="requestReturn(order)">24小时内退货申请</button>
              <select v-model.number="review.stars" style="width:120px">
                <option v-for="i in 5" :value="i">{{i}} 星</option>
              </select>
              <input v-model="review.content" placeholder="文字评价">
              <button @click="submitReview(order, products[item.productId])">评价商家</button>
            </div>
          </div>
        </div>
        <p v-if="msg" class="notice">{{msg}}</p>
      </section>
    </main>`
};

export default OrderDetail;
