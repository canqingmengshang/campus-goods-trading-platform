import {api, session, money} from '../core.js';

const UserCenter = {
    data() {
        return {user: {}};
    },
    async mounted() {
        await this.load();
    },
    methods: {
        money,
        async load() {
            this.user = await api.get('/api/users/' + session.current.id);
        }
    },
    template: `
    <main class="container">
      <section class="split">
        <div class="panel">
          <h2>个人中心</h2>
          <p>用户：{{user.username}}</p>
          <p>姓名：{{user.realName || '-'}}</p>
          <p>手机号：{{user.phone || '-'}}</p>
          <p>邮箱：{{user.email || '-'}}</p>
          <p>城市：{{user.city || '-'}}</p>
          <p>性别：{{user.gender || '-'}}</p>
          <p>角色：{{user.role}}</p>
          <p>状态：{{user.status}}</p>
        </div>
        <div class="panel">
          <h2>钱包</h2>
          <p class="price">￥{{money(user.wallet)}}</p>
          <p>积分总数：{{user.points}}</p>
          <p>银行账号：{{user.bankAccount || '-'}}</p>
        </div>
      </section>
    </main>`
};

export default UserCenter;
