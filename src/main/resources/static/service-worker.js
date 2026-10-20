"use strict";
// No fetch handler and no cache: authenticated pages/API/files never enter an offline cache.
const notificationId = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
self.addEventListener("push", event => {
  let id;
  try { id = event.data.json().id; } catch { return; }
  if (typeof id !== "string" || !notificationId.test(id)) return;
  event.waitUntil(self.registration.showNotification("Deadline Tracker", {
    body: "Bạn có thông báo mới. Mở hộp thông báo để xem.", tag: id,
    data: { id }, icon: "/favicon.svg", renotify: false
  }));
});
self.addEventListener("notificationclick", event => {
  event.notification.close();
  const id = event.notification.data?.id;
  if (typeof id !== "string" || !notificationId.test(id)) return;
  event.waitUntil(clients.openWindow(new URL("/notifications/" + id, self.location.origin).href));
});
