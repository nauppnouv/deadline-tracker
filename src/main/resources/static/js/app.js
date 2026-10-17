"use strict";
const invite = document.getElementById("invitation-link");
const copy = document.querySelector("[data-copy-invite]");
if (invite && copy) {
  invite.value = window.location.href;
  copy.addEventListener("click", async () => {
    const result = document.getElementById("copy-result");
    try { await navigator.clipboard.writeText(invite.value); result.textContent = "Đã sao chép liên kết mời."; }
    catch { invite.focus(); invite.select(); result.textContent = "Hãy nhấn Ctrl+C hoặc chọn Sao chép trên thiết bị của bạn."; }
  });
}
