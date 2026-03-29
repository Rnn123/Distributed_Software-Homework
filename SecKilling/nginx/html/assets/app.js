const state = {
    token: "",
    currentUser: null
};

const authState = document.getElementById("authState");
const eventLog = document.getElementById("eventLog");
const productList = document.getElementById("productList");

document.getElementById("loginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formData = new FormData(event.target);
    const payload = Object.fromEntries(formData.entries());

    const result = await request("/api/users/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
    });

    if (result.code === 0) {
        state.token = result.data.token;
        state.currentUser = result.data.user;
        authState.textContent = `已登录：${state.currentUser.username}\nToken: ${state.token}`;
        appendLog(`登录成功，用户 ${state.currentUser.username}`);
    } else {
        authState.textContent = `登录失败：${result.msg}`;
    }
});

document.getElementById("refreshProducts").addEventListener("click", loadProducts);

async function loadProducts() {
    const result = await request("/api/products");
    if (result.code !== 0) {
        productList.innerHTML = `<p>加载失败：${result.msg}</p>`;
        return;
    }

    productList.innerHTML = "";
    for (const product of result.data) {
        const inventoryResult = await request(`/api/inventory/${product.id}`);
        const inventory = inventoryResult.code === 0 ? inventoryResult.data : { availableStock: "N/A" };
        const item = document.createElement("article");
        item.className = "product-item";
        item.innerHTML = `
            <img src="${product.imageUrl || ""}" alt="${product.name}">
            <span class="badge">库存 ${inventory.availableStock}</span>
            <h3>${product.name}</h3>
            <p>${product.description || ""}</p>
            <div class="price-row">
                <span class="seckill-price">￥${product.seckillPrice}</span>
                <span class="original-price">￥${product.originalPrice}</span>
            </div>
            <button data-id="${product.id}">立即秒杀</button>
        `;
        item.querySelector("button").addEventListener("click", () => doSeckill(product.id, product.name));
        productList.appendChild(item);
    }
}

async function doSeckill(productId, productName) {
    if (!state.token) {
        appendLog("请先登录后再发起秒杀。");
        return;
    }

    const result = await request(`/api/seckill/${productId}`, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${state.token}`
        }
    });

    if (result.code !== 0) {
        appendLog(`秒杀 ${productName} 失败：${result.msg}`);
        return;
    }

    appendLog(`秒杀 ${productName} 已提交，订单ID：${result.data.orderId}`);
    pollStatus(result.data.orderId, productName);
}

async function pollStatus(orderId, productName) {
    for (let i = 0; i < 6; i += 1) {
        const result = await request(`/api/seckill/status/${orderId}`);
        if (result.code === 0 && result.data !== "PROCESSING") {
            appendLog(`订单 ${orderId} (${productName}) 状态：${result.data}`);
            return;
        }
        await sleep(1000);
    }
    appendLog(`订单 ${orderId} 仍在处理中，请稍后到订单接口查询。`);
}

async function request(url, options = {}) {
    const response = await fetch(url, options);
    return response.json();
}

function appendLog(message) {
    const time = new Date().toLocaleTimeString();
    eventLog.textContent = `[${time}] ${message}\n${eventLog.textContent}`.trim();
}

function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
}

loadProducts();
