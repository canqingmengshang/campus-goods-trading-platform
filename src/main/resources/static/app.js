const {createApp} = Vue;

const api = {
    async request(url, options = {}) {
        const response = await fetch(url, {...options, credentials: 'include'});
        const data = await response.json().catch(() => ({}));
        if (!response.ok && response.status === 401) {
            session.clear();
            if (location.pathname !== '/login') nav('/login');
        }
        if (!response.ok) throw new Error(data.message || '操作失败');
        return data;
    },
    get(url) {
        return this.request(url);
    },
    post(url, body) {
        return this.request(url, {
            method: 'POST',
            headers: body instanceof FormData ? undefined : {'Content-Type': 'application/json'},
            body: body instanceof FormData ? body : JSON.stringify(body || {})
        });
    },
    put(url, body) {
        return this.request(url, {method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body || {})});
    }
};

const session = {
    current: JSON.parse(localStorage.getItem('campus-user') || 'null'),
    save(user) {
        this.current = user;
        localStorage.setItem('campus-user', JSON.stringify(user));
    },
    clear() {
        this.current = null;
        localStorage.removeItem('campus-user');
    }
};

const money = value => Number(value || 0).toFixed(2);

function nav(path) {
    history.pushState({}, '', path);
    window.dispatchEvent(new Event('popstate'));
}

function isLoggedIn() {
    return !!session.current;
}

function isAdmin() {
    return session.current?.role === 'ADMIN';
}

function isMerchant() {
    return session.current?.role === 'MERCHANT';
}

function safePath(path) {
    if (path === '/login') return path;
    if (!isLoggedIn()) return '/login';
    if (path.startsWith('/admin/')) return isAdmin() ? path : '/home';
    if (path.startsWith('/merchant/')) return isMerchant() ? path : '/home';
    return path === '/' ? '/home' : path;
}

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
                this.registerForm = {username: '', password: '', realName: '', phone: '', email: '', city: '', gender: '', bankAccount: '', shopName: ''};
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
            <div class="captcha"><input v-model="loginForm.captcha"><img :src="captcha.image" @click="loadCaptcha"></div>
          </label>
          <button class="full" @click="login">登录系统</button>
        </div>
        <div v-else class="form-grid">
          <label class="full">注册类型
            <select v-model="role"><option value="BUYER">普通用户</option><option value="MERCHANT">商家</option></select>
          </label>
          <label>用户名<input v-model="registerForm.username"></label>
          <label>密码<input v-model="registerForm.password" type="password"></label>
          <label>姓名<input v-model="registerForm.realName"></label>
          <label>手机号<input v-model="registerForm.phone" maxlength="11" placeholder="11位手机号"></label>
          <label v-if="role==='BUYER'">邮箱<input v-model="registerForm.email" type="email"></label>
          <label v-if="role==='BUYER'">城市<input v-model="registerForm.city"></label>
          <label>性别
            <select v-model="registerForm.gender"><option value="">请选择</option><option>男</option><option>女</option><option>其他</option></select>
          </label>
          <label>银行账号<input v-model="registerForm.bankAccount" maxlength="16" placeholder="16位数字"></label>
          <label v-if="role==='MERCHANT'">店铺名<input v-model="registerForm.shopName"></label>
          <label v-if="role==='MERCHANT'">营业执照<input type="file" @change="license=$event.target.files[0]"></label>
          <label v-if="role==='MERCHANT'">身份证<input type="file" @change="idCard=$event.target.files[0]"></label>
          <button class="full" @click="register">提交注册</button>
        </div>
      </section>
    </div>`
};

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
          <div class="panel"><b>推荐位轮播</b><p class="muted">山地车、小冰箱、考研资料正在热卖。</p></div>
          <div class="panel"><b>交易保障</b><p class="muted">商品审核、商家审核、24小时退货申请、五星评价闭环。</p></div>
        </div>
      </section>
      <section class="panel">
        <div class="search-row">
          <input v-model="filters.keyword" placeholder="按商品名称搜索">
          <button @click="load">搜索</button>
        </div>
        <div class="filter-row">
          <select v-model="filters.sort"><option value="">默认排序</option><option value="price">价格</option><option value="sales">销量</option><option value="rate">好评率</option></select>
          <input v-model="filters.minPrice" placeholder="最低价">
          <input v-model="filters.maxPrice" placeholder="最高价">
          <button @click="load">筛选</button>
        </div>
      </section>
      <section class="grid" style="margin-top:16px">
        <article v-for="p in products" :key="p.id" class="card">
          <img class="product-img" :src="p.photos[0]" @click="detail(p.id)">
          <div class="card-body">
            <div class="title-row"><b>{{p.name}}</b><span class="badge">{{p.condition}}</span></div>
            <div><span class="price">¥{{money(p.salePrice)}}</span> <span class="muted"><s>¥{{money(p.originalPrice)}}</s></span></div>
            <p class="muted">{{p.merchantName}} · 销量 {{p.sales}} · 好评 {{p.favorableRate}}%</p>
            <button @click="detail(p.id)">查看详情</button>
          </div>
        </article>
      </section>
    </main>`
};

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
          <p><span class="price">¥{{money(product.salePrice)}}</span> <span class="muted">原价 <s>¥{{money(product.originalPrice)}}</s></span></p>
          <p><span class="badge">{{product.condition}}</span> <span class="badge">{{product.negotiable ? '可议价' : '不议价'}}</span></p>
          <p class="muted">尺寸：{{product.size}} · 库存：{{product.stock}} · 历史销量：{{product.sales}}</p>
          <p>{{product.usageGuide}}</p>
          <div class="panel">
            <b>{{product.merchantName}}</b>
            <p class="muted">商品好评率 {{product.favorableRate}}%，支持查看商家历史评价。</p>
            <button class="secondary" @click="nav('/shop/' + product.merchantId)">进入店铺</button>
          </div>
          <label>数量<input type="number" min="1" v-model.number="quantity"></label>
          <div class="actions"><button @click="addCart(false)">加入购物车</button><button class="danger" @click="addCart(true)">立即购买</button></div>
          <p v-if="msg" class="notice">{{msg}}</p>
        </div>
      </section>
    </main>`
};

const Cart = {
    data() {
        return {items: [], products: {}, user: {}, pointsUsed: 0, msg: ''};
    },
    computed: {
        total() {
            return this.items.filter(i => i.selected).reduce((sum, item) => sum + Number(this.products[item.productId]?.salePrice || 0) * item.quantity, 0);
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
            for (const item of this.items) this.products[item.productId] = await api.get('/api/products/' + item.productId);
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
                this.msg = '钱包余额不足，当前余额 ¥' + this.money(this.user.wallet) + '，应付 ¥' + this.money(this.payable);
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
    <main class="container"><section class="panel">
      <div class="title-row"><h2>购物车</h2><span class="muted">钱包 ¥{{money(user.wallet)}} · 积分 {{user.points}}</span></div>
      <table class="table"><thead><tr><th>选择</th><th>商品</th><th>商家</th><th>单价</th><th>数量</th><th>小计</th></tr></thead>
      <tbody><tr v-for="item in items" :key="item.productId">
        <td><input type="checkbox" v-model="item.selected"></td><td>{{products[item.productId]?.name}}</td><td>{{products[item.productId]?.merchantName}}</td>
        <td>¥{{money(products[item.productId]?.salePrice)}}</td><td><input type="number" min="1" v-model.number="item.quantity" style="width:90px"></td>
        <td>¥{{money(Number(products[item.productId]?.salePrice || 0) * item.quantity)}}</td>
      </tr></tbody></table>
      <div class="actions"><label style="max-width:220px">积分抵扣<input type="number" min="0" :max="user.points" v-model.number="pointsUsed"></label><b>合计 ¥{{money(total)}}，应付 ¥{{money(payable)}}</b><button @click="checkout">一键下单并扣款</button></div>
      <p class="muted">100 积分 = 1 元。</p><p v-if="msg" class="notice">{{msg}}</p>
    </section></main>`
};

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
          <p class="price">¥{{money(user.wallet)}}</p>
          <p>积分总数：{{user.points}}</p>
          <p>银行账号：{{user.bankAccount || '-'}}</p>
        </div>
      </section>
    </main>`
};

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
            for (const order of this.orders) for (const item of order.items) this.products[item.productId] = await api.get('/api/products/' + item.productId);
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
            await api.post('/api/users/' + session.current.id + '/reviews', {orderId: order.id, merchantId: product.merchantId, productId: product.id, stars: this.review.stars, content: this.review.content});
            this.msg = '评价已提交';
            await this.load();
        }
    },
    template: `
    <main class="container">
      <section class="panel"><h2>订单详情</h2>
        <div v-for="order in orders" :key="order.id" class="panel" style="margin:12px 0">
          <div class="title-row"><b>订单 {{order.id}}</b><span class="badge">{{order.status}}</span></div>
          <p>实付 ¥{{money(order.totalAmount)}}，积分抵扣 {{order.pointsUsed}}</p>
          <div v-for="item in order.items" :key="item.productId">
            {{products[item.productId]?.name}} x {{item.quantity}}
            <div class="actions"><button class="secondary" v-if="order.status==='PAID'" @click="received(order)">确认收货</button><button class="danger" v-if="order.status==='RECEIVED'" @click="requestReturn(order)">24小时内退货申请</button><select v-model.number="review.stars" style="width:120px"><option v-for="i in 5" :value="i">{{i}} 星</option></select><input v-model="review.content" placeholder="文字评价"><button @click="submitReview(order, products[item.productId])">评价商家</button></div>
          </div>
        </div><p v-if="msg" class="notice">{{msg}}</p>
      </section>
    </main>`
};

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
            return this.products.flatMap(product => (product.reviews || []).map(review => ({...review, productName: product.name}))).slice(0, 8);
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
            <div class="title-row"><b>{{p.name}}</b><span class="badge">{{p.condition}}</span></div>
            <p class="price">¥{{money(p.salePrice)}}</p>
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
        <div class="title-row"><h2>历史评价</h2><span class="muted">{{reviews.length ? '最近评价' : '暂无评价'}}</span></div>
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
    <main class="container"><section class="panel">
      <div class="title-row"><h2>商家工作台</h2><button @click="nav('/merchant/publish')">发布商品</button></div>
      <table class="table"><thead><tr><th>商品</th><th>价格</th><th>库存</th><th>销量</th><th>状态</th><th>操作</th></tr></thead>
      <tbody><tr v-for="p in products" :key="p.id"><td>{{p.name}}</td><td>¥{{money(p.salePrice)}}</td><td>{{p.stock}}</td><td>{{p.sales}}</td><td><span class="badge">{{p.status}}</span></td><td><button class="secondary" @click="offShelf(p.id)">下架</button></td></tr></tbody></table>
    </section></main>`
};

const PublishProduct = {
    data() {
        return {msg: '', form: {name: '', category: '图书', originalPrice: 0, salePrice: 0, size: '', photos: [], usageGuide: '', negotiable: true, stock: 1, condition: '九成新'}};
    },
    methods: {
        addPhoto(event) {
            const upload = new FormData();
            Array.from(event.target.files).forEach(file => upload.append('files', file));
            api.post('/api/uploads', upload).then(urls => this.form.photos = urls).catch(error => this.msg = error.message);
        },
        async save() {
            const product = await api.post('/api/merchants/' + session.current.id + '/products', this.form);
            this.msg = '商品已提交审核，编号 ' + product.id;
        }
    },
    template: `
    <main class="container"><section class="panel"><h2>发布 / 编辑商品</h2>
      <div class="form-grid">
        <label>名称<input v-model="form.name"></label><label>类别<input v-model="form.category"></label>
        <label>原价<input type="number" v-model.number="form.originalPrice"></label><label>折后价<input type="number" v-model.number="form.salePrice"></label>
        <label>尺寸<input v-model="form.size"></label><label>库存数量<input type="number" min="1" v-model.number="form.stock"></label>
        <label>新旧程度<select v-model="form.condition"><option>全新</option><option>九成新</option><option>七成新</option><option>有明显使用痕迹</option></select></label>
        <label>是否议价<select v-model="form.negotiable"><option :value="true">可议价</option><option :value="false">不议价</option></select></label>
        <label class="full">照片（支持多张）<input type="file" multiple @change="addPhoto"></label>
        <label class="full">使用说明<textarea v-model="form.usageGuide"></textarea></label>
        <button class="full" @click="save">提交审核</button>
      </div><p v-if="msg" class="notice">{{msg}}</p>
    </section></main>`
};

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
    <main class="container"><section class="split">
      <div class="panel"><h2>待审核用户 / 商家</h2><table class="table">
        <thead><tr><th>账号</th><th>身份信息</th><th>银行账号</th><th>证件图片</th><th>操作</th></tr></thead>
        <tbody><tr v-for="u in merchants" :key="u.id">
          <td>{{u.username}}<br><span class="badge">{{u.role}}</span></td>
          <td>{{u.realName}} · {{u.gender}}<br><span class="muted">{{u.phone}} {{u.email}} {{u.city}}</span><br><span v-if="u.role==='MERCHANT'">{{u.shopName}}</span></td>
          <td>{{u.bankAccount}}</td>
          <td><img v-if="u.licenseImage" :src="u.licenseImage" style="width:72px"> <img v-if="u.idCardImage" :src="u.idCardImage" style="width:72px"></td>
          <td><button @click="approveUser(u.id)">批准生效</button></td>
        </tr></tbody>
      </table></div>
      <div class="panel"><h2>待审核商品</h2><table class="table"><tr v-for="p in products" :key="p.id"><td>{{p.name}}</td><td>{{p.merchantName}}</td><td><button @click="approveProduct(p.id)">批准上架</button></td></tr></table></div>
    </section></main>`
};

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
    <main class="container"><section class="panel"><h2>用户与惩罚管理</h2>
      <table class="table"><thead><tr><th>ID</th><th>用户</th><th>角色</th><th>钱包</th><th>状态</th><th>等级/费率</th><th>操作</th></tr></thead>
      <tbody><tr v-for="u in users" :key="u.id"><td>{{u.id}}</td><td>{{u.username}}<br><span class="muted">{{u.shopName}}</span></td><td>{{u.role}}</td><td>¥{{money(u.wallet)}}</td><td>{{u.status}}</td><td>{{u.merchantLevel}}级 / {{u.feeRate}}%</td><td><button class="secondary" @click="punish(u.id,'LIMITED')">限制发布</button> <button class="danger" @click="punish(u.id,'BLACKLISTED')">拉黑</button> <button @click="punish(u.id,'ACTIVE')">恢复</button></td></tr></tbody></table>
    </section>
    <section class="split" style="margin-top:16px">
      <div class="panel"><h2>充值功能</h2><label>用户ID<input type="number" v-model.number="recharge.userId"></label><label>充值金额<input type="number" v-model.number="recharge.amount"></label><div class="actions"><button @click="doRecharge">确认充值</button></div></div>
      <div class="panel"><h2>费率设置</h2><label>商家ID<input type="number" v-model.number="fee.merchantId"></label><label>等级<select v-model.number="fee.level"><option v-for="i in 5" :value="i">{{i}} 级</option></select></label><p class="muted">1-5级对应 0.1% ~ 1% 费率。</p><div class="actions"><button @click="setFee">保存费率</button></div></div>
    </section></main>`
};

const routes = [
    [/^\/login$/, LoginRegister],
    [/^\/$|^\/home$/, Home],
    [/^\/product\/\d+$/, ProductDetail],
    [/^\/cart$/, Cart],
    [/^\/orders$/, OrderDetail],
    [/^\/user$/, UserCenter],
    [/^\/shop\/\d+$/, Shop],
    [/^\/merchant\/products$/, MerchantProducts],
    [/^\/merchant\/publish$/, PublishProduct],
    [/^\/admin\/audit$/, AdminAudit],
    [/^\/admin\/users$/, AdminUsers]
];

async function bootstrap() {
    try {
        const user = await api.get('/api/session');
        session.save(user);
    } catch (error) {
        session.clear();
        if (location.pathname !== '/login') history.replaceState({}, '', '/login');
    }

    createApp({
    data() {
        return {path: safePath(location.pathname), user: session.current};
    },
    computed: {
        view() {
            return (routes.find(([pattern]) => pattern.test(this.path)) || routes[1])[1];
        },
        showTopbar() {
            return this.view !== LoginRegister;
        },
        canAdmin() {
            return this.user?.role === 'ADMIN';
        },
        canMerchant() {
            return this.user?.role === 'MERCHANT';
        }
    },
    mounted() {
        if (location.pathname !== this.path) history.replaceState({}, '', this.path);
        window.addEventListener('popstate', () => {
            this.user = session.current;
            const nextPath = safePath(location.pathname);
            if (nextPath !== location.pathname) history.replaceState({}, '', nextPath);
            this.path = nextPath;
        });
    },
    methods: {
        nav(path) {
            nav(safePath(path));
        },
        logout() {
            session.clear();
            this.user = null;
            api.post('/api/logout').catch(() => {});
            nav('/login');
        }
    },
    template: `
    <div class="app-shell">
      <header v-if="showTopbar" class="topbar">
        <div class="brand" @click="nav('/home')">校园闲置交易平台</div>
        <nav class="nav">
          <a :class="{active:path==='/home'}" @click.prevent="nav('/home')" href="/home">首页</a>
          <a :class="{active:path==='/cart'}" @click.prevent="nav('/cart')" href="/cart">购物车</a>
          <a :class="{active:path==='/orders'}" @click.prevent="nav('/orders')" href="/orders">订单详情</a>
          <a :class="{active:path==='/user'}" @click.prevent="nav('/user')" href="/user">个人中心</a>
          <a v-if="canMerchant" :class="{active:path==='/merchant/products'}" @click.prevent="nav('/merchant/products')" href="/merchant/products">商家工作台</a>
          <a v-if="canAdmin" :class="{active:path==='/admin/audit'}" @click.prevent="nav('/admin/audit')" href="/admin/audit">审核管理</a>
          <a v-if="canAdmin" :class="{active:path==='/admin/users'}" @click.prevent="nav('/admin/users')" href="/admin/users">用户管理</a>
          <a @click.prevent="logout" href="/login">退出登录</a>
        </nav>
      </header>
      <component :is="view"></component>
    </div>`
    }).mount('#app');
}

bootstrap();
