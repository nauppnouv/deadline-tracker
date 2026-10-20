"use strict";
(() => {
  const panel = document.getElementById("push-panel");
  if (!panel) return;
  const status = document.getElementById("push-status");
  const enable = document.getElementById("push-enable");
  const list = document.getElementById("push-devices");
  const token = document.querySelector('meta[name="_csrf"]').content;
  const header = document.querySelector('meta[name="_csrf_header"]').content;
  let config;
  async function post(path, body) {
    const response = await fetch(path, { method: "POST", credentials: "same-origin", cache: "no-store",
      headers: { [header]: token, ...(body ? { "Content-Type": "application/json" } : {}) },
      body: body ? JSON.stringify(body) : undefined });
    if (!response.ok) throw new Error("Không lưu được lựa chọn thông báo. Hãy tải lại hoặc thử lại sau.");
    return response.json();
  }
  function keyBytes(key) {
    const raw = atob(key.replace(/-/g, "+").replace(/_/g, "/") + "=".repeat((4 - key.length % 4) % 4));
    return Uint8Array.from(raw, c => c.charCodeAt(0));
  }
  async function refresh() {
    const response = await fetch("/api/push/config", { credentials: "same-origin", cache: "no-store" });
    if (!response.ok) throw new Error("Không tải được thiết bị thông báo. Hãy đăng nhập lại.");
    config = await response.json(); list.replaceChildren();
    if (!config.configured) { enable.disabled = true; status.textContent = "Máy chủ chưa bật thông báo desktop. Bạn vẫn xem được hộp thông báo."; }
    for (const [index, device] of config.devices.entries()) {
      const item = document.createElement("li");
      item.append(document.createTextNode("Thiết bị " + (index + 1) + (device.id === config.current ? " (trình duyệt này)" : "") + (device.enabled ? " — Đang bật " : " — Đã tắt ")));
      if (device.enabled) {
        const button = document.createElement("button");button.type = "button";button.className = "secondary";button.textContent = "Tắt thiết bị";
        button.addEventListener("click", async () => {
          button.disabled = true;
          try {
            await post("/api/push/devices/" + device.id + "/disable");
            if (device.id === config.current && "serviceWorker" in navigator) {
              const registration = await navigator.serviceWorker.getRegistration("/");
              const subscription = await registration?.pushManager.getSubscription();
              if (subscription) await subscription.unsubscribe();
            }
            status.textContent = "Đã thu hồi quyền gửi tới thiết bị.";await refresh();
          } catch (error) { status.textContent = error.message;button.disabled = false; }
        });item.append(button);
      }
      list.append(item);
    }
  }
  if (!window.isSecureContext || !("serviceWorker" in navigator) || !("PushManager" in window) || !("Notification" in window)) {
    enable.disabled = true;status.textContent = "Trình duyệt chưa hỗ trợ thông báo desktop ở địa chỉ này. Hãy dùng hộp thông báo.";return;
  }
  status.textContent = Notification.permission === "denied" ? "Bạn đã chặn thông báo. Có thể đổi lựa chọn trong cài đặt trình duyệt." : "Chỉ xin quyền khi bạn bấm Bật thông báo.";
  enable.addEventListener("click", async () => {
    if (!config?.configured) return;
    enable.disabled = true;
    let subscription;
    try {
      if (Notification.permission === "denied") throw new Error("Thông báo đang bị chặn. Hãy đổi quyền trong cài đặt trình duyệt nếu bạn muốn bật.");
      const permission = Notification.permission === "granted" ? "granted" : await Notification.requestPermission();
      if (permission !== "granted") throw new Error("Chưa cấp quyền. Bạn vẫn có thể dùng hộp thông báo.");
      const registration = await navigator.serviceWorker.register("/service-worker.js", { scope: "/" });
      await navigator.serviceWorker.ready;
      const previous = await registration.pushManager.getSubscription();
      if (config.current && config.devices.some(d => d.id === config.current && d.enabled)) await post("/api/push/devices/" + config.current + "/disable");
      if (previous) await previous.unsubscribe();
      subscription = await registration.pushManager.subscribe({ userVisibleOnly: true, applicationServerKey: keyBytes(config.publicKey) });
      const data = subscription.toJSON();
      await post("/api/push/devices", { endpoint: data.endpoint, p256dh: data.keys.p256dh, auth: data.keys.auth });
      status.textContent = "Đã bật thông báo cho trình duyệt này.";await refresh();
    } catch (error) {
      if (subscription) { try { await subscription.unsubscribe(); } catch { /* server registration did not succeed */ } }
      status.textContent = error.message || "Chưa bật được thông báo. Bạn vẫn xem được hộp thông báo.";
    } finally { enable.disabled = !config?.configured; }
  });
  refresh().catch(error => { status.textContent = error.message;enable.disabled = true; });
})();
