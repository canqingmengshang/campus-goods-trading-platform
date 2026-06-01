import {api, money} from '../core.js';

const AdminUsers = {
    data() {
        return {users: [], recharge: {userId: 2, amount: 100}, fee: {merchantId: 3, level: 3}};
    },
    async mounted() {
        await this.load();
    },
    methods: {
        money,
        async load() {
            this.users = await api.get('/api/admin/users');
        },
        async doRecharge() {
            await api.post('/api/admin/recharge', this.recharge);
            await this.load();
        },
        async punish(userId, status) {
            await api.post('/api/admin/punish', {userId, status});
            await this.load();
        },
        async setFee() {
            await api.post('/api/admin/fee', this.fee);
            await this.load();
        }
    },
    template: `
    <main class="container">
      <section class="panel">
        <h2>用户与处罚管理</h2>
        <table class="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>用户</th>
              <th>角色</th>
              <th>钱包</th>
              <th>状态</th>
              <th>等级/费率</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="u in users" :key="u.id">
              <td>{{u.id}}</td>
              <td>{{u.username}}<br><span class="muted">{{u.shopName}}</span></td>
              <td>{{u.role}}</td>
              <td>￥{{money(u.wallet)}}</td>
              <td>{{u.status}}</td>
              <td>{{u.merchantLevel}}级 / {{u.feeRate}}%</td>
              <td>
                <button class="secondary" @click="punish(u.id,'LIMITED')">限制发布</button>
                <button class="danger" @click="punish(u.id,'BLACKLISTED')">拉黑</button>
                <button @click="punish(u.id,'ACTIVE')">恢复</button>
              </td>
            </tr>
          </tbody>
        </table>
      </section>
      <section class="split" style="margin-top:16px">
        <div class="panel">
          <h2>充值功能</h2>
          <label>用户ID<input type="number" v-model.number="recharge.userId"></label>
          <label>充值金额<input type="number" v-model.number="recharge.amount"></label>
          <div class="actions"><button @click="doRecharge">确认充值</button></div>
        </div>
        <div class="panel">
          <h2>费率设置</h2>
          <label>商家ID<input type="number" v-model.number="fee.merchantId"></label>
          <label>等级
            <select v-model.number="fee.level">
              <option v-for="i in 5" :value="i">{{i}} 级</option>
            </select>
          </label>
          <p class="muted">1-5级对应 0.1% ~ 1% 费率。</p>
          <div class="actions"><button @click="setFee">保存费率</button></div>
        </div>
      </section>
    </main>`
};

export default AdminUsers;
