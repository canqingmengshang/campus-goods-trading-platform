export const api = {
    async request(url, options = {}) {
        const response = await fetch(url, options);
        const data = await response.json().catch(() => ({}));
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

export const session = {
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

export const money = value => Number(value || 0).toFixed(2);

export function nav(path) {
    history.pushState({}, '', path);
    window.dispatchEvent(new Event('popstate'));
}

export function isLoggedIn() {
    return !!session.current;
}

export function isAdmin() {
    return session.current?.role === 'ADMIN';
}

export function isMerchant() {
    return session.current?.role === 'MERCHANT';
}

export function safePath(path) {
    if (path === '/login') return path;
    if (!isLoggedIn()) return '/login';
    if (path.startsWith('/admin/')) return isAdmin() ? path : '/home';
    if (path.startsWith('/merchant/')) return isMerchant() ? path : '/home';
    return path === '/' ? '/home' : path;
}
