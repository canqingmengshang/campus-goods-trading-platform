import {api, session} from '../core.js';

const PublishProduct = {
    data() {
        return {
            msg: '',
            form: {
                name: '',
                category: '图书',
                originalPrice: 0,
                salePrice: 0,
                size: '',
                photos: [],
                usageGuide: '',
                negotiable: true,
                stock: 1,
                condition: '九成新'
            }
        };
    },
    methods: {
        addPhoto(event) {
            const upload = new FormData();
            Array.from(event.target.files).forEach(file => upload.append('files', file));
            api.post('/api/uploads', upload)
                .then(urls => this.form.photos = urls)
                .catch(error => this.msg = error.message);
        },
        async save() {
            const product = await api.post('/api/merchants/' + session.current.id + '/products', this.form);
            this.msg = '商品已提交审核，编号 ' + product.id;
        }
    },
    template: `
    <main class="container">
      <section class="panel">
        <h2>发布 / 编辑商品</h2>
        <div class="form-grid">
          <label>名称<input v-model="form.name"></label>
          <label>类别<input v-model="form.category"></label>
          <label>原价<input type="number" v-model.number="form.originalPrice"></label>
          <label>折后价<input type="number" v-model.number="form.salePrice"></label>
          <label>尺寸<input v-model="form.size"></label>
          <label>库存数量<input type="number" min="1" v-model.number="form.stock"></label>
          <label>新旧程度
            <select v-model="form.condition">
              <option>全新</option>
              <option>九成新</option>
              <option>七成新</option>
              <option>有明显使用痕迹</option>
            </select>
          </label>
          <label>是否议价
            <select v-model="form.negotiable">
              <option :value="true">可议价</option>
              <option :value="false">不议价</option>
            </select>
          </label>
          <label class="full">照片（支持多张）<input type="file" multiple @change="addPhoto"></label>
          <label class="full">使用说明<textarea v-model="form.usageGuide"></textarea></label>
          <button class="full" @click="save">提交审核</button>
        </div>
        <p v-if="msg" class="notice">{{msg}}</p>
      </section>
    </main>`
};

export default PublishProduct;
