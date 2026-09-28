document.addEventListener('DOMContentLoaded', () => {
    // Tab Navigation
    const navButtons = document.querySelectorAll('.nav-btn');
    const tabPanes = document.querySelectorAll('.tab-pane');

    // Camera & Scanner Elements
    const webcamStream = document.getElementById('webcam-stream');
    const faceCanvas = document.getElementById('face-canvas');
    const cameraPlaceholder = document.getElementById('camera-placeholder');
    const btnStartCamera = document.getElementById('btn-start-camera');
    const btnManualScan = document.getElementById('btn-manual-scan');
    const autoScanToggle = document.getElementById('auto-scan-toggle');
    const scanIndicator = document.getElementById('scan-indicator');
    const scanOverlay = document.getElementById('scan-overlay');

    // Match Result Elements
    const matchEmpty = document.getElementById('match-empty');
    const matchResult = document.getElementById('match-result');
    const resultPhoto = document.getElementById('result-photo');
    const resultName = document.getElementById('result-name');
    const resultDept = document.getElementById('result-dept');
    const resultConfidence = document.getElementById('result-confidence');
    const resultConfidenceBar = document.getElementById('result-confidence-bar');
    const resultTimestamp = document.getElementById('result-timestamp');
    const miniLogsList = document.getElementById('mini-logs-list');

    // Registration Elements
    const regWebcamStream = document.getElementById('reg-webcam-stream');
    const regSnapshotImg = document.getElementById('reg-snapshot-img');
    const btnRegCapture = document.getElementById('btn-reg-capture');
    const btnRegRetake = document.getElementById('btn-reg-retake');
    const enrollmentForm = document.getElementById('enrollment-form');
    const enrollmentAlert = document.getElementById('enrollment-alert');
    const userCountBadge = document.getElementById('user-count-badge');

    // Dashboard Elements
    const usersGrid = document.getElementById('users-grid');
    const logsTableBody = document.getElementById('logs-table-body');
    const btnRefreshUsers = document.getElementById('btn-refresh-users');
    const btnRefreshLogs = document.getElementById('btn-refresh-logs');

    // State Variables
    let mediaStream = null;
    let autoScanInterval = null;
    let isProcessingScan = false;
    let currentRegistrationSnapshot = null;

    // --- TAB SWITCHING ---
    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');
            
            navButtons.forEach(b => b.classList.remove('active'));
            tabPanes.forEach(p => p.classList.remove('active'));

            btn.classList.add('active');
            document.getElementById(targetTab).classList.add('active');

            if (targetTab === 'roster-tab') {
                loadUsers();
                loadLogs();
            } else if (targetTab === 'admin-tab') {
                loadAdminPortal();
            }
        });
    });

    // --- WEBCAM INITIALIZATION ---
    async function startCamera() {
        try {
            mediaStream = await navigator.mediaDevices.getUserMedia({
                video: { width: { ideal: 640 }, height: { ideal: 480 }, facingMode: 'user' },
                audio: false
            });

            webcamStream.srcObject = mediaStream;
            regWebcamStream.srcObject = mediaStream;

            cameraPlaceholder.classList.add('hidden');
            btnManualScan.disabled = false;

            updateScanIndicator('Ready for face scan', 'active');
            
            if (autoScanToggle.checked) {
                startAutoScan();
            }
        } catch (err) {
            console.error("Camera access error:", err);
            alert("Unable to access webcam. Please check browser camera permissions.");
        }
    }

    btnStartCamera.addEventListener('click', startCamera);

    // --- FACE RECOGNITION SCANNING ---
    function captureFrameBase64() {
        if (!webcamStream || webcamStream.readyState !== 4) return null;
        
        const canvas = document.createElement('canvas');
        canvas.width = webcamStream.videoWidth || 640;
        canvas.height = webcamStream.videoHeight || 480;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(webcamStream, 0, 0, canvas.width, canvas.height);
        
        return canvas.toDataURL('image/png');
    }

    async function performFaceScan() {
        if (isProcessingScan || !mediaStream) return;
        
        const frameBase64 = captureFrameBase64();
        if (!frameBase64) return;

        isProcessingScan = true;
        updateScanIndicator('Analyzing facial features...', 'active');
        scanOverlay.classList.add('scanning');

        try {
            const response = await fetch('/api/recognize', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ imageBase64: frameBase64 })
            });

            const data = await response.json();

            if (data.recognized) {
                displayRecognitionSuccess(data, frameBase64);
                updateScanIndicator(`Matched: ${data.name} (${data.confidenceScore.toFixed(1)}%)`, 'success');
            } else {
                updateScanIndicator(data.message || 'No target match found', 'idle');
            }
        } catch (err) {
            console.error("Scan error:", err);
            updateScanIndicator('Recognition error', 'idle');
        } finally {
            isProcessingScan = false;
            scanOverlay.classList.remove('scanning');
        }
    }

    function displayRecognitionSuccess(data, capturedFrame) {
        matchEmpty.classList.add('hidden');
        matchResult.classList.remove('hidden');

        resultName.textContent = data.name;
        resultDept.innerHTML = `<i class="fa-solid fa-building"></i> ${data.department || 'General'}`;
        resultPhoto.src = capturedFrame;

        const score = data.confidenceScore || 0;
        resultConfidence.textContent = `${score.toFixed(1)}%`;
        resultConfidenceBar.style.width = `${Math.min(100, score)}%`;

        const now = new Date();
        resultTimestamp.textContent = now.toLocaleTimeString();

        // Add to mini activity log
        addMiniLogEntry(data.name, data.department, score, now);
    }

    function addMiniLogEntry(name, dept, confidence, date) {
        const item = document.createElement('div');
        item.className = 'log-item';
        item.innerHTML = `
            <div>
                <strong>${name}</strong>
                <span class="text-muted"> (${dept || 'Gen'})</span>
            </div>
            <div>
                <span class="status-tag success">${confidence.toFixed(0)}% Match</span>
                <span class="text-muted" style="margin-left: 8px;">${date.toLocaleTimeString()}</span>
            </div>
        `;

        if (miniLogsList.querySelector('.text-muted')) {
            miniLogsList.innerHTML = '';
        }
        miniLogsList.prepend(item);
    }

    function updateScanIndicator(text, state) {
        scanIndicator.className = `scan-indicator ${state}`;
        scanIndicator.innerHTML = `<i class="fa-solid fa-circle-notch ${state === 'active' ? 'fa-spin' : ''}"></i> ${text}`;
    }

    // Auto-scan Toggle
    function startAutoScan() {
        stopAutoScan();
        autoScanInterval = setInterval(() => {
            if (autoScanToggle.checked && mediaStream) {
                performFaceScan();
            }
        }, 3000);
    }

    function stopAutoScan() {
        if (autoScanInterval) {
            clearInterval(autoScanInterval);
            autoScanInterval = null;
        }
    }

    autoScanToggle.addEventListener('change', () => {
        if (autoScanToggle.checked) {
            startAutoScan();
        } else {
            stopAutoScan();
        }
    });

    btnManualScan.addEventListener('click', performFaceScan);

    // --- USER REGISTRATION ENROLLMENT ---
    btnRegCapture.addEventListener('click', () => {
        if (!regWebcamStream || regWebcamStream.readyState !== 4) {
            alert("Please start the camera first!");
            return;
        }

        const canvas = document.createElement('canvas');
        canvas.width = regWebcamStream.videoWidth || 640;
        canvas.height = regWebcamStream.videoHeight || 480;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(regWebcamStream, 0, 0, canvas.width, canvas.height);

        currentRegistrationSnapshot = canvas.toDataURL('image/png');
        regSnapshotImg.src = currentRegistrationSnapshot;

        regWebcamStream.classList.add('hidden');
        regSnapshotImg.classList.remove('hidden');
        btnRegCapture.classList.add('hidden');
        btnRegRetake.classList.remove('hidden');
    });

    btnRegRetake.addEventListener('click', () => {
        currentRegistrationSnapshot = null;
        regSnapshotImg.classList.add('hidden');
        regWebcamStream.classList.remove('hidden');
        btnRegRetake.classList.add('hidden');
        btnRegCapture.classList.remove('hidden');
    });

    enrollmentForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        if (!currentRegistrationSnapshot) {
            showEnrollmentAlert("Please capture a face photo snapshot before submitting.", "error");
            return;
        }

        const name = document.getElementById('reg-name').value;
        const email = document.getElementById('reg-email').value;
        const department = document.getElementById('reg-dept').value;

        const btnSubmit = document.getElementById('btn-enroll-submit');
        btnSubmit.disabled = true;
        btnSubmit.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Enrolling User...`;

        try {
            const response = await fetch('/api/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    name: name,
                    email: email,
                    department: department,
                    imageBase64: currentRegistrationSnapshot
                })
            });

            const data = await response.json();

            if (response.ok && data.success) {
                showEnrollmentAlert(`User '${name}' registered & face template trained successfully!`, "success");
                enrollmentForm.reset();
                btnRegRetake.click();
                fetchSystemHealth();
            } else {
                showEnrollmentAlert(data.error || "Enrollment failed", "error");
            }
        } catch (err) {
            console.error("Registration error:", err);
            showEnrollmentAlert("Network or server error during enrollment", "error");
        } finally {
            btnSubmit.disabled = false;
            btnSubmit.innerHTML = `<i class="fa-solid fa-floppy-disk"></i> Register & Train Face`;
        }
    });

    function showEnrollmentAlert(msg, type) {
        enrollmentAlert.className = `alert alert-${type === 'success' ? 'success' : 'error'}`;
        enrollmentAlert.textContent = msg;
        enrollmentAlert.classList.remove('hidden');
    }

    // --- DASHBOARD ROSTER & LOGS ---
    async function loadUsers() {
        try {
            const res = await fetch('/api/users');
            const users = await res.json();

            userCountBadge.textContent = `${users.length} Enrolled`;

            if (users.length === 0) {
                usersGrid.innerHTML = `<p class="text-muted">No users enrolled yet. Go to 'Enroll User' tab to register.</p>`;
                return;
            }

            usersGrid.innerHTML = users.map(u => `
                <div class="user-card">
                    <img src="${u.photoBase64 || 'https://via.placeholder.com/100'}" alt="${u.name}">
                    <div class="user-info">
                        <h4>${u.name}</h4>
                        <p><i class="fa-solid fa-building"></i> ${u.department || 'General'}</p>
                        <p><i class="fa-solid fa-envelope"></i> ${u.email}</p>
                    </div>
                </div>
            `).join('');
        } catch (err) {
            console.error("Error loading users:", err);
        }
    }

    async function loadLogs() {
        try {
            const res = await fetch('/api/logs');
            const logs = await res.json();

            if (logs.length === 0) {
                logsTableBody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No attendance recognition logs recorded yet.</td></tr>`;
                return;
            }

            logsTableBody.innerHTML = logs.map(log => `
                <tr>
                    <td>#${log.id}</td>
                    <td><strong>${log.userName || 'Unknown'}</strong></td>
                    <td>${log.department || 'General'}</td>
                    <td><strong style="color: var(--success);">${(log.confidenceScore || 0).toFixed(1)}%</strong></td>
                    <td><span class="status-tag success">${log.status || 'SUCCESS'}</span></td>
                    <td>${log.timestamp ? new Date(log.timestamp).toLocaleString() : 'Just now'}</td>
                </tr>
            `).join('');
        } catch (err) {
            console.error("Error loading logs:", err);
        }
    }

    async function fetchSystemHealth() {
        try {
            const res = await fetch('/api/health');
            const data = await res.json();
            if (data.registeredUsers !== undefined) {
                userCountBadge.textContent = `${data.registeredUsers} Enrolled`;
            }
        } catch (ignored) {}
    }

    btnRefreshUsers.addEventListener('click', loadUsers);
    btnRefreshLogs.addEventListener('click', loadLogs);

    // Initial Health Check
    fetchSystemHealth();

    // --- ADMIN PORTAL LOGIC ---
    let allAdminUsers = [];

    const adminTotalCompanies = document.getElementById('admin-total-companies');
    const adminTotalUsers = document.getElementById('admin-total-users');
    const companyBadgeGrid = document.getElementById('company-badge-grid');
    const adminDeptFilter = document.getElementById('admin-dept-filter');
    const adminTableBody = document.getElementById('admin-table-body');
    const btnRefreshAdmin = document.getElementById('btn-refresh-admin');

    // Modal elements
    const editUserModal = document.getElementById('edit-user-modal');
    const editUserForm = document.getElementById('edit-user-form');
    const editUserId = document.getElementById('edit-user-id');
    const editUserName = document.getElementById('edit-user-name');
    const editUserEmail = document.getElementById('edit-user-email');
    const editUserDept = document.getElementById('edit-user-dept');
    const editUserAlert = document.getElementById('edit-user-alert');
    const btnCloseModal = document.getElementById('btn-close-modal');
    const btnCancelEdit = document.getElementById('btn-cancel-edit');

    async function loadAdminPortal() {
        await loadAdminStats();
        await loadAdminUsers();
    }

    async function loadAdminStats() {
        try {
            const res = await fetch('/api/admin/stats');
            const stats = await res.json();

            if (adminTotalUsers) adminTotalUsers.textContent = stats.totalUsers || 0;
            if (adminTotalCompanies) adminTotalCompanies.textContent = stats.totalCompanies || 0;

            const companyCounts = stats.companyCounts || [];
            if (companyBadgeGrid) {
                if (companyCounts.length === 0) {
                    companyBadgeGrid.innerHTML = `<p class="text-muted">No company data available.</p>`;
                } else {
                    companyBadgeGrid.innerHTML = companyCounts.map(c => `
                        <div class="company-card">
                            <div class="company-name"><i class="fa-solid fa-building"></i> ${c.company || 'General'}</div>
                            <div class="company-count">${c.userCount} <span style="font-size: 0.85rem; font-weight: normal; color: var(--text-muted);">Users</span></div>
                        </div>
                    `).join('');
                }
            }

            // Populate Filter dropdown options
            if (adminDeptFilter) {
                const currentSelected = adminDeptFilter.value;
                adminDeptFilter.innerHTML = `<option value="ALL">All Companies / Depts (${stats.totalUsers})</option>` + 
                    companyCounts.map(c => `<option value="${c.company}">${c.company} (${c.userCount})</option>`).join('');
                adminDeptFilter.value = currentSelected || 'ALL';
            }

        } catch (err) {
            console.error("Error loading admin stats:", err);
        }
    }

    async function loadAdminUsers() {
        try {
            const res = await fetch('/api/users');
            allAdminUsers = await res.json();
            renderAdminTable();
        } catch (err) {
            console.error("Error loading admin users:", err);
        }
    }

    function renderAdminTable() {
        if (!adminTableBody) return;

        const filter = adminDeptFilter ? adminDeptFilter.value : 'ALL';
        const filteredUsers = filter === 'ALL' ? allAdminUsers : allAdminUsers.filter(u => (u.department || 'General') === filter);

        if (filteredUsers.length === 0) {
            adminTableBody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No users found for selected company/department.</td></tr>`;
            return;
        }

        adminTableBody.innerHTML = filteredUsers.map(u => `
            <tr>
                <td>
                    <div style="display: flex; align-items: center; gap: 10px;">
                        <img src="${u.photoBase64 || 'https://via.placeholder.com/40'}" style="width: 36px; height: 36px; border-radius: 50%; object-fit: cover; border: 1px solid var(--primary);" alt="${u.name}">
                        <strong>${u.name}</strong>
                    </div>
                </td>
                <td>#${u.id}</td>
                <td>${u.email}</td>
                <td><span class="status-tag" style="background: rgba(99, 102, 241, 0.15); color: #818cf8;">${u.department || 'General'}</span></td>
                <td>${u.createdAt ? new Date(u.createdAt).toLocaleDateString() : 'N/A'}</td>
                <td>
                    <div style="display: flex; gap: 8px;">
                        <button class="btn btn-sm btn-warning btn-edit" data-id="${u.id}"><i class="fa-solid fa-pen-to-square"></i> Edit</button>
                        <button class="btn btn-sm btn-danger btn-delete" data-id="${u.id}"><i class="fa-solid fa-trash"></i> Delete</button>
                    </div>
                </td>
            </tr>
        `).join('');

        // Attach action handlers
        adminTableBody.querySelectorAll('.btn-edit').forEach(btn => {
            btn.addEventListener('click', () => openEditModal(btn.getAttribute('data-id')));
        });

        adminTableBody.querySelectorAll('.btn-delete').forEach(btn => {
            btn.addEventListener('click', () => deleteUser(btn.getAttribute('data-id')));
        });
    }

    if (adminDeptFilter) adminDeptFilter.addEventListener('change', renderAdminTable);
    if (btnRefreshAdmin) btnRefreshAdmin.addEventListener('click', loadAdminPortal);

    // Edit Modal Logic
    function openEditModal(userId) {
        const user = allAdminUsers.find(u => u.id == userId);
        if (!user || !editUserModal) return;

        editUserId.value = user.id;
        editUserName.value = user.name;
        editUserEmail.value = user.email;
        editUserDept.value = user.department || 'General';
        if (editUserAlert) editUserAlert.classList.add('hidden');

        editUserModal.classList.remove('hidden');
    }

    function closeModal() {
        if (editUserModal) editUserModal.classList.add('hidden');
    }

    if (btnCloseModal) btnCloseModal.addEventListener('click', closeModal);
    if (btnCancelEdit) btnCancelEdit.addEventListener('click', closeModal);

    if (editUserForm) {
        editUserForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const id = editUserId.value;
            const name = editUserName.value;
            const email = editUserEmail.value;
            const department = editUserDept.value;

            const btnSave = document.getElementById('btn-save-edit');
            btnSave.disabled = true;
            btnSave.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> Saving...`;

            try {
                const res = await fetch(`/api/users/${id}`, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, email, department })
                });

                const data = await res.json();
                if (res.ok && data.success) {
                    closeModal();
                    loadAdminPortal();
                    loadUsers();
                    fetchSystemHealth();
                } else {
                    if (editUserAlert) {
                        editUserAlert.className = "alert alert-error";
                        editUserAlert.textContent = data.error || "Failed to update user details.";
                        editUserAlert.classList.remove('hidden');
                    }
                }
            } catch (err) {
                console.error("Error updating user:", err);
            } finally {
                btnSave.disabled = false;
                btnSave.innerHTML = `<i class="fa-solid fa-floppy-disk"></i> Save Changes`;
            }
        });
    }

    // Delete User
    async function deleteUser(userId) {
        const user = allAdminUsers.find(u => u.id == userId);
        const name = user ? user.name : `User #${userId}`;

        if (!confirm(`Are you sure you want to delete '${name}'? This action cannot be undone.`)) {
            return;
        }

        try {
            const res = await fetch(`/api/users/${userId}`, { method: 'DELETE' });
            const data = await res.json();

            if (res.ok && data.success) {
                loadAdminPortal();
                loadUsers();
                fetchSystemHealth();
            } else {
                alert(data.error || "Failed to delete user.");
            }
        } catch (err) {
            console.error("Error deleting user:", err);
            alert("Network error while deleting user.");
        }
    }
});
