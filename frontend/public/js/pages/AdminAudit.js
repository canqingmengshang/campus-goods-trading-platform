import {api} from '../core.js';

const AdminAudit = {
    data() {
        return {merchants: [], products: []};
    },
    async mounted() {
        await this.load();
    },
    methods: {
        async load() {
            this.merchants = await api.get('/api/admin/pending-merchants');
            this.products = await api.get('/api/admin/products?status=AUDITING');
        },
        async approveUser(id) {
            await api.post('/api/admin/users/' + id + '/approve');
            await this.load();
        },
        async approveProduct(id) {
            await api.post('/api/products/' + id + '/status?status=PUBLISHED');
            await this.load();
        }
    },
    template: `
    <main class="container">
      <section class="split">
        <div class="panel">
          <h2>待审核用户 / 商家</h2>
          <table class="table">
            <thead>
              <tr>
                <th>账号</th>
                <th>身份信息</th>
                <th>银行账号</th>
                <th>证件图片</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in merchants" :key="u.id">
                <td>{{u.username}}<br><span class="badge">{{u.role}}</span></td>
                <td>
                  {{u.realName}} · {{u.gender}}<br>
                  <span class="muted">{{u.phone}} {{u.email}} {{u.city}}</span><br>
                  <span v-if="u.role==='MERCHANT'">{{u.shopName}}</span>
                </td>
                <td>{{u.bankAccount}}</td>
                <td>
                  <img v-if="u.licenseImage" :src="u.licenseImage" style="width:72px">
                  <img v-if="u.idCardImage" :src="u.idCardImage" style="width:72px">
                </td>
                <td><button @click="approveUser(u.id)">批准生效</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="panel">
          <h2>待审核商品</h2>
          <table class="table">
            <tr v-for="p in products" :key="p.id">
              <td>{{p.name}}</td>
              <td>{{p.merchantName}}</td>
              <td><button @click="approveProduct(p.id)">批准上架</button></td>
            </tr>
          </table>
        </div>
      </section>
    </main>`
};

export default AdminAudit;
