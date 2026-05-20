import {api, session, nav} from '../core.js';

const LoginRegister = {
    data() {
        return {
            mode: 'login',
            role: 'BUYER',
            captcha: {},
            msg: '',
            loginForm: {username: 'buyer', password: '123456', captcha: ''},
            registerForm: {
                username: '',
                password: '',
                realName: '',
                phone: '',
                email: '',
                city: '',
                gender: '',
                bankAccount: '',
                shopName: ''
            },
            license: null,
            idCard: null
        };
    },
    async mounted() {
        await this.loadCaptcha();
    },
    methods: {
        async loadCaptcha() {
            this.captcha = await api.get('/api/captcha');
            this.loginForm.captcha = '';
        },
        async login() {
            try {
                const user = await api.post('/api/login', this.loginForm);
                session.save(user);
                nav('/home');
            } catch (error) {
                this.msg = error.message;
                await this.loadCaptcha();
            }
        },
        async register() {
            try {
                if (!/^1\d{10}$/.test(this.registerForm.phone)) {
                    this.msg = '手机号必须为11位数字且以1开头';
                    return;
                }
                const form = new FormData();
                Object.entries(this.registerForm).forEach(([key, value]) => form.append(key, value || ''));
                form.append('role', this.role);
                if (this.license) form.append('license', this.license);
                if (this.idCard) form.append('idCard', this.idCard);
                await api.post('/api/register', form);
                this.msg = '注册申请已提交，请等待管理员审核。审核通过后方可登录使用。';
                this.mode = 'login';
                this.registerForm = {
                    username: '',
                    password: '',
                    realName: '',
                    phone: '',
                    email: '',
                    city: '',
                    gender: '',
                    bankAccount: '',
                    shopName: ''
                };
                this.license = null;
                this.idCard = null;
                await this.loadCaptcha();
            } catch (error) {
                this.msg = error.message;
            }
        }
    },
    template: `
    <div class="login-wrap">
      <section class="login-art">
        <h1>校园闲置交易平台</h1>
        <p>面向校园场景的闲置物品发布、审核、购买、钱包扣款、积分抵扣和评价管理系统。</p>
      </section>
      <section class="panel login-panel">
        <div class="tabs">
          <button :class="{secondary: mode!=='login'}" @click="mode='login'">登录</button>
          <button :class="{secondary: mode!=='register'}" @click="mode='register'">注册</button>
        </div>
        <div v-if="msg" class="notice">{{msg}}</div>
        <div v-if="mode==='login'" class="form-grid">
          <label>用户名<input v-model="loginForm.username"></label>
          <label>密码<input v-model="loginForm.password" type="password"></label>
          <label class="full">图形验证码
            <div class="captcha">
              <input v-model="loginForm.captcha">
              <img :src="captcha.image" @click="loadCaptcha">
            </div>
          </label>
          <button class="full" @click="login">登录系统</button>
        </div>
        <div v-else class="form-grid">
          <label class="full">注册类型
            <select v-model="role">
              <option value="BUYER">普通用户</option>
              <option value="MERCHANT">商家</option>
            </select>
          </label>
          <label>用户名<input v-model="registerForm.username"></label>
          <label>密码<input v-model="registerForm.password" type="password"></label>
          <label>姓名<input v-model="registerForm.realName"></label>
          <label>手机号<input v-model="registerForm.phone" maxlength="11" placeholder="11位手机号"></label>
          <label v-if="role==='BUYER'">邮箱<input v-model="registerForm.email" type="email"></label>
          <label v-if="role==='BUYER'">城市<input v-model="registerForm.city"></label>
          <label>性别
            <select v-model="registerForm.gender">
              <option value="">请选择</option>
              <option>男</option>
              <option>女</option>
              <option>其他</option>
            </select>
          </label>
          <label>银行账号<input v-model="registerForm.bankAccount" maxlength="16" placeholder="16位数字"></label>
          <label v-if="role==='MERCHANT'">店铺名称<input v-model="registerForm.shopName"></label>
          <label v-if="role==='MERCHANT'">营业执照<input type="file" @change="license=$event.target.files[0]"></label>
          <label v-if="role==='MERCHANT'">身份证<input type="file" @change="idCard=$event.target.files[0]"></label>
          <button class="full" @click="register">提交注册</button>
        </div>
      </section>
    </div>`
};

export default LoginRegister;
