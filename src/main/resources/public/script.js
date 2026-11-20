// Тонкий клієнт: лише надсилає запити до /api/Payroll і показує відповіді.
// Правил перевірки даних і оформлення (кольорів, шрифтів, розмірів) тут немає.

const API = '/api/Payroll';
const rows = document.getElementById('employee-rows');
const form = document.getElementById('employee-form');
const idField = document.getElementById('employeeId');
const nameField = document.getElementById('fullName');
const cancelButton = document.getElementById('cancel-button');
const messageBox = document.getElementById('message');

// null — режим додавання; інакше — ідентифікатор працівника, якого виправляємо
let editingId = null;

function showMessage(text, ok) {
  messageBox.textContent = text;
  messageBox.classList.remove('hidden', 'message-success', 'message-error');
  messageBox.classList.add(ok ? 'message-success' : 'message-error');
}

async function readMessage(response) {
  try {
    const body = await response.json();
    return body.message || '';
  } catch (e) {
    return 'Не вдалося прочитати відповідь сервера.';
  }
}

async function loadEmployees() {
  try {
    const response = await fetch(API);
    if (!response.ok) {
      showMessage(await readMessage(response), false);
      return;
    }
    const body = await response.json();
    rows.innerHTML = '';
    for (const employee of body.employees) {
      rows.appendChild(buildRow(employee));
    }
  } catch (e) {
    showMessage('Сервер недоступний.', false);
  }
}

function buildRow(employee) {
  const tr = document.createElement('tr');
  tr.appendChild(textCell(employee.employeeId));
  tr.appendChild(textCell(employee.fullName));

  const actions = document.createElement('td');
  actions.className = 'p-2';
  const edit = document.createElement('button');
  edit.textContent = 'Змінити';
  edit.className = 'px-3 py-1 mr-2 button-secondary';
  edit.dataset.id = employee.employeeId;
  edit.addEventListener('click', () => startEditing(employee));
  const remove = document.createElement('button');
  remove.textContent = 'Видалити';
  remove.className = 'px-3 py-1 button-delete';
  remove.dataset.id = employee.employeeId;
  remove.addEventListener('click', () => removeEmployee(employee));
  actions.appendChild(edit);
  actions.appendChild(remove);
  tr.appendChild(actions);
  return tr;
}

function textCell(text) {
  const td = document.createElement('td');
  td.className = 'p-2';
  td.textContent = text;
  return td;
}

function startEditing(employee) {
  editingId = employee.employeeId;
  idField.value = employee.employeeId;
  idField.readOnly = true;
  nameField.value = employee.fullName;
  cancelButton.classList.remove('hidden');
}

function resetForm() {
  editingId = null;
  form.reset();
  idField.readOnly = false;
  cancelButton.classList.add('hidden');
}

async function removeEmployee(employee) {
  if (!confirm('Видалити працівника "' + employee.fullName + '" (' + employee.employeeId + ')?')) {
    return;
  }
  const response = await fetch(API + '/' + encodeURIComponent(employee.employeeId),
      { method: 'DELETE' });
  showMessage(await readMessage(response), response.ok);
  if (response.ok && editingId === employee.employeeId) {
    resetForm();
  }
  await loadEmployees();
}

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  const payload = JSON.stringify({ employeeId: idField.value, fullName: nameField.value });
  const url = editingId === null ? API : API + '/' + encodeURIComponent(editingId);
  const method = editingId === null ? 'POST' : 'PUT';
  try {
    const response = await fetch(url, {
      method: method,
      headers: { 'Content-Type': 'application/json' },
      body: payload
    });
    showMessage(await readMessage(response), response.ok);
    if (response.ok) {
      resetForm();
    }
  } catch (e) {
    showMessage('Сервер недоступний.', false);
  }
  await loadEmployees();
});

cancelButton.addEventListener('click', resetForm);

loadEmployees();
