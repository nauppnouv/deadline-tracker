"use strict";
// Suggestions use only the active members rendered for this authorized group.
for (const select of document.querySelectorAll('select[name="mentionIds"]')) {
  const choices = Array.from(select.options);
  const search = document.createElement("input");
  search.type = "search"; search.placeholder = "Tìm thành viên để nhắc";
  search.setAttribute("aria-label", "Tìm thành viên để nhắc");
  select.before(search);
  function refresh() {
    const query = search.value.toLocaleLowerCase("vi");
    const selected = choices.filter(option => option.selected);
    const matches = choices.filter(option => !option.selected && option.text.toLocaleLowerCase("vi").includes(query)).slice(0, Math.max(0, 10 - selected.length));
    select.replaceChildren(...selected, ...matches);
  }
  search.addEventListener("input", refresh);
  refresh();
}
