import {api, session, money} from '../core.js';

const Cart = {
    data() {
        return {items: [], products: {}, user: {}, pointsUsed: 0, msg: ''};
    },
    computed: {
        total() {
            return this.items.filter(i => i.selected)
                .reduce((sum, item) => sum + Number(this.products[item.productId]?.salePrice || 0) * item.quantity, 0);
        },
        payable() {
            return Math.max(0, this.total - this.pointsUsed / 100);
        }
    },
    watch: {
        pointsUsed(value) {
            const points = Number(value || 0);
            const maxPoints = Number(this.user.points || 0);
            if (points < 0) {
                this.pointsUsed = 0;
                return;
            }
            if (points > maxPoints) {
                this.pointsUsed = maxPoints;
                this.msg = '积分不足，已自动调整为当前可用积分 ' + maxPoints;
            }
        }
    },
    async mounted() {
        await this.load();
    },
    methods: {
        money,
        async load() {
            this.user = await api.get('/api/users/' + session.current.id);
            this.items = await api.get('/api/users/' + session.current.id + '/cart');
            for (const item of this.items) {
                this.products[item.productId] = await api.get('/api/products/' + item.productId);
            }
        },
        async save() {
            this.items = await api.put('/api/users/' + session.current.id + '/cart', this.items);
        },
        async checkout() {
            if (this.pointsUsed > Number(this.user.points || 0)) {
                this.pointsUsed = Number(this.user.points || 0);
                this.msg = '积分不足，已自动调整为当前可用积分 ' + this.pointsUsed;
                return;
            }
            if (this.payable > Number(this.user.wallet || 0)) {
                this.msg = '钱包余额不足，当前余额 ￥' + this.money(this.user.wallet) + '，应付 ￥' + this.money(this.payable);
                return;
            }
            await this.save();
            try {
                const order = await api.post('/api/users/' + session.current.id + '/checkout?pointsUsed=' + this.pointsUsed);
                this.msg = '下单成功，订单号 ' + order.id;
                await this.load();
            } catch (error) {
                this.msg = error.message;
                await this.load();
            }
        }
    },
    template: `
    <main class="container">
      <section class="panel">
        <div class="title-row">
          <h2>购物车</h2>
          <span class="muted">钱包 ￥{{money(user.wallet)}} · 积分 {{user.points}}</span>
        </div>
        <table class="table">
          <thead>
            <tr>
              <th>选择</th>
              <th>商品</th>
              <th>商家</th>
              <th>单价</th>
              <th>数量</th>
              <th>小计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.productId">
              <td><input type="checkbox" v-model="item.selected"></td>
              <td>{{products[item.productId]?.name}}</td>
              <td>{{products[item.productId]?.merchantName}}</td>
              <td>￥{{money(products[item.productId]?.salePrice)}}</td>
              <td><input type="number" min="1" v-model.number="item.quantity" style="width:90px"></td>
              <td>￥{{money(Number(products[item.productId]?.salePrice || 0) * item.quantity)}}</td>
            </tr>
          </tbody>
        </table>
        <div class="actions">
          <label style="max-width:220px">积分抵扣
            <input type="number" min="0" :max="user.points" v-model.number="pointsUsed">
          </label>
          <b>合计 ￥{{money(total)}}，应付 ￥{{money(payable)}}</b>
          <button @click="checkout">一键下单并扣款</button>
        </div>
        <p class="muted">100 积分 = 1 元。</p>
        <p v-if="msg" class="notice">{{msg}}</p>
      </section>
    </main>`
};

export default Cart;
