/* Code Review Agent - Vanilla JS Client Application */
const API_BASE_URL = window.location.origin;

// State
let currentUser = null;

try {
    currentUser = JSON.parse(localStorage.getItem('cra_user')) || null;
} catch (e) {
    currentUser = null;
}

// Initialize Theme
function initTheme() {
    const savedTheme = localStorage.getItem('cra_theme') || 'dark';
    document.documentElement.setAttribute('data-theme', savedTheme);
}

function toggleTheme() {
    const currentTheme = document.documentElement.getAttribute('data-theme');
    const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('cra_theme', newTheme);
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
    return null;
}

// Helper to check if current page is protected
function isProtectedPage() {
    const path = window.location.pathname.toLowerCase();
    const protectedPages = ['/dashboard.html', '/review.html', '/memory.html', '/settings.html', '/history.html'];
    return protectedPages.some(page => path.endsWith(page));
}

// API Helper with Session Credentials & CSRF Protection Support
async function apiCall(endpoint, method = 'GET', body = null, isMultipart = false) {
    const headers = {};
    if (!isMultipart) {
        headers['Content-Type'] = 'application/json';
    }

    const xsrfToken = getCookie('XSRF-TOKEN');
    if (xsrfToken) {
        headers['X-XSRF-TOKEN'] = xsrfToken;
    }

    const options = {
        method,
        headers,
        credentials: 'same-origin',
    };

    if (body) {
        options.body = isMultipart ? body : JSON.stringify(body);
    }

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
        
        if (response.status === 401) {
            console.warn('Authentication expired or unauthorized access to:', endpoint);
            localStorage.removeItem('cra_user');
            currentUser = null;
            if (isProtectedPage()) {
                window.location.href = '/login.html';
            }
            throw new Error('Authentication required. Please log in.');
        }

        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.message || data.error || 'API Request failed');
        }
        return data;
    } catch (err) {
        console.error(`API Error on ${endpoint}:`, err);
        throw err;
    }
}

// Helper to extract initials
function getUserInitials(name) {
    if (!name) return 'U';
    const parts = name.trim().split(' ');
    if (parts.length >= 2) {
        return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
}

// Get Role Badge HTML (Individual Developer Model)
function getRoleBadgeHtml(user) {
    if (!user) return '';
    return `<span class="role-badge badge-individual"><i class="fa-solid fa-user"></i> Developer</span>`;
}

// Page Protection Check
async function checkPageProtection() {
    if (isProtectedPage()) {
        try {
            const res = await apiCall('/api/auth/me');
            if (res && res.data) {
                currentUser = res.data;
                localStorage.setItem('cra_user', JSON.stringify(currentUser));
                return true;
            }
        } catch (err) {
            console.warn('Unauthorized access attempt to protected page:', window.location.pathname);
            window.location.href = '/login.html';
            return false;
        }
    }
    return true;
}

// Logout Handler
async function logout() {
    try {
        await apiCall('/api/auth/logout', 'POST');
    } catch (e) {
        console.warn('Logout notification error:', e.message);
    }
    localStorage.removeItem('cra_user');
    currentUser = null;
    window.location.href = '/login.html';
}

// Dynamic Navigation & UI Bootstrapper
function renderNavigation() {
    const path = window.location.pathname.toLowerCase();
    const isAuthenticated = !!currentUser;

    // Sidebar visibility
    const sidebar = document.querySelector('.sidebar');
    if (sidebar) {
        if (!isAuthenticated && (path === '/' || path.endsWith('/index.html') || path.endsWith('/login.html') || path.endsWith('/register.html'))) {
            sidebar.style.display = 'none';
        } else {
            sidebar.style.display = 'flex';
        }
    }

    // Navbar User profile area
    const navbarActions = document.querySelector('.navbar-actions');
    if (navbarActions) {
        const existingAuthBtn = document.getElementById('auth-btn');
        let profileDiv = document.getElementById('navbar-user-profile');

        if (isAuthenticated) {
            if (existingAuthBtn) existingAuthBtn.style.display = 'none';

            if (!profileDiv) {
                profileDiv = document.createElement('div');
                profileDiv.id = 'navbar-user-profile';
                profileDiv.className = 'user-avatar-badge';
                navbarActions.insertBefore(profileDiv, navbarActions.firstChild);
            }

            const displayName = currentUser.fullName || currentUser.username || 'Developer';
            const initials = getUserInitials(displayName);
            const badgeHtml = getRoleBadgeHtml(currentUser);

            profileDiv.innerHTML = `
                <div class="user-avatar" title="${displayName}">${initials}</div>
                <div style="display: flex; flex-direction: column; text-align: left; line-height: 1.2;">
                    <span style="font-weight: 600; font-size: 0.85rem; color: var(--text-primary);">${displayName}</span>
                    ${badgeHtml}
                </div>
                <button class="btn btn-secondary" onclick="logout()" title="Logout" style="margin-left: 0.5rem; padding: 0.4rem 0.75rem; font-size: 0.8rem;">
                    <i class="fa-solid fa-right-from-bracket"></i> Logout
                </button>
            `;
        } else {
            if (profileDiv) profileDiv.remove();
            if (existingAuthBtn) {
                existingAuthBtn.style.display = 'inline-flex';
                existingAuthBtn.textContent = 'Login / Register';
                existingAuthBtn.href = '/login.html';
            }
        }
    }

    // Display user name placeholders in pages
    const userDisplayNameElem = document.getElementById('user-display-name');
    if (userDisplayNameElem) {
        userDisplayNameElem.textContent = isAuthenticated ? (currentUser.fullName || currentUser.username) : '';
    }
}

// Notification Bell Controller
async function initNotificationBell() {
    const bellBtn = document.getElementById('notification-bell-btn');
    const dropdown = document.getElementById('notification-dropdown');
    const badge = document.getElementById('notification-badge');
    const list = document.getElementById('notification-list');

    if (!bellBtn || !dropdown || !list) return;

    bellBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        dropdown.classList.toggle('active');
    });

    document.addEventListener('click', (e) => {
        if (!dropdown.contains(e.target) && e.target !== bellBtn) {
            dropdown.classList.remove('active');
        }
    });

    try {
        const res = await apiCall('/api/notifications');
        const notifications = res.data || [];

        const unreadCount = notifications.filter(n => !n.isRead).length;
        if (badge) {
            badge.textContent = unreadCount;
            badge.style.display = unreadCount > 0 ? 'inline-block' : 'none';
        }

        if (notifications.length === 0) {
            list.innerHTML = '<li class="notification-item">No notifications yet</li>';
        } else {
            list.innerHTML = notifications.slice(0, 5).map(n => `
                <li class="notification-item ${!n.isRead ? 'unread' : ''}" onclick="markNotificationRead('${n.id}')">
                    <strong>${n.title || 'Notification'}</strong>
                    <div>${n.message || ''}</div>
                </li>
            `).join('');
        }
    } catch (err) {
        console.warn('Could not fetch notifications:', err.message);
    }
}

async function markNotificationRead(id) {
    try {
        await apiCall(`/api/notifications/${id}/read`, 'PUT');
        initNotificationBell();
    } catch (err) {
        console.error(err);
    }
}

// Toast Notifications Helper
function showToast(message, type = 'success') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    const icon = type === 'success' ? 'fa-circle-check' : (type === 'error' ? 'fa-circle-exclamation' : 'fa-info-circle');
    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.animation = 'slideIn 0.3s reverse forwards';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

// Global UI Bootstrapper
document.addEventListener('DOMContentLoaded', async () => {
    initTheme();

    const themeToggleBtn = document.getElementById('theme-toggle-btn');
    if (themeToggleBtn) {
        themeToggleBtn.addEventListener('click', toggleTheme);
    }

    const isProtectedValid = await checkPageProtection();
    if (isProtectedValid) {
        renderNavigation();
        initNotificationBell();
    }
});
