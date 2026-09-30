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

    // Initialize Mobile Navigation (Hamburger & Mobile Drawer)
    initMobileNavigation();
}

// Mobile Navigation Drawer & Hamburger Menu Controller
function initMobileNavigation() {
    const navbar = document.querySelector('.navbar');
    if (!navbar) return;

    let hamburgerBtn = document.getElementById('mobile-menu-toggle-btn');
    if (!hamburgerBtn) {
        let leftNav = navbar.querySelector('.navbar-left-container');
        if (!leftNav) {
            leftNav = document.createElement('div');
            leftNav.className = 'navbar-left-container';
            leftNav.style.display = 'flex';
            leftNav.style.alignItems = 'center';
            leftNav.style.gap = '0.75rem';

            navbar.insertBefore(leftNav, navbar.firstChild);
        }

        hamburgerBtn = document.createElement('button');
        hamburgerBtn.id = 'mobile-menu-toggle-btn';
        hamburgerBtn.className = 'btn-icon mobile-hamburger-btn';
        hamburgerBtn.setAttribute('title', 'Open Menu');
        hamburgerBtn.setAttribute('aria-label', 'Open Navigation Menu');
        hamburgerBtn.innerHTML = '<i class="fa-solid fa-bars"></i>';
        leftNav.appendChild(hamburgerBtn);

        const navbarTitle = navbar.querySelector('.navbar-title');
        if (navbarTitle && !leftNav.querySelector('.mobile-brand')) {
            const brandElem = document.createElement('div');
            brandElem.className = 'mobile-brand';
            brandElem.innerHTML = `
                <div class="sidebar-logo" style="width: 32px; height: 32px; font-size: 0.9rem;"><i class="fa-solid fa-code"></i></div>
                <span style="font-weight: 700; font-size: 1.05rem; color: var(--text-primary);">CodeReview.AI</span>
            `;
            leftNav.appendChild(brandElem);
        }
    }

    let drawer = document.getElementById('mobile-nav-drawer');
    let backdrop = document.getElementById('mobile-nav-backdrop');

    if (!drawer) {
        backdrop = document.createElement('div');
        backdrop.id = 'mobile-nav-backdrop';
        backdrop.className = 'mobile-nav-backdrop';
        document.body.appendChild(backdrop);

        drawer = document.createElement('aside');
        drawer.id = 'mobile-nav-drawer';
        drawer.className = 'mobile-nav-drawer';
        document.body.appendChild(drawer);

        backdrop.addEventListener('click', closeMobileDrawer);
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') closeMobileDrawer();
        });
    }

    updateMobileDrawerContent(drawer);

    if (hamburgerBtn) {
        hamburgerBtn.onclick = (e) => {
            e.stopPropagation();
            toggleMobileDrawer();
        };
    }
}

function updateMobileDrawerContent(drawer) {
    if (!drawer) return;
    const path = window.location.pathname.toLowerCase();
    const isAuthenticated = !!currentUser;
    const displayName = currentUser ? (currentUser.fullName || currentUser.username || 'Developer') : '';
    const initials = currentUser ? getUserInitials(displayName) : '';
    const badgeHtml = currentUser ? getRoleBadgeHtml(currentUser) : '';

    drawer.innerHTML = `
        <div class="mobile-drawer-header">
            <div style="display: flex; align-items: center; gap: 0.6rem;">
                <div class="sidebar-logo" style="width: 32px; height: 32px; font-size: 0.9rem;"><i class="fa-solid fa-code"></i></div>
                <span style="font-size: 1.1rem; font-weight: 700; color: var(--text-primary);">CodeReview.AI</span>
            </div>
            <button class="btn-icon" id="mobile-drawer-close-btn" title="Close Menu" onclick="closeMobileDrawer()">&times;</button>
        </div>

        ${isAuthenticated ? `
            <div class="mobile-drawer-user">
                <div class="user-avatar">${initials}</div>
                <div style="display: flex; flex-direction: column; gap: 2px;">
                    <span style="font-weight: 600; font-size: 0.9rem; color: var(--text-primary);">${displayName}</span>
                    ${badgeHtml}
                </div>
            </div>
        ` : ''}

        <ul class="mobile-drawer-menu">
            <li class="mobile-drawer-item ${path.endsWith('/index.html') || path === '/' ? 'active' : ''}">
                <a href="/index.html"><i class="fa-solid fa-house"></i> <span>Home</span></a>
            </li>
            <li class="mobile-drawer-item ${path.endsWith('/dashboard.html') ? 'active' : ''}">
                <a href="/dashboard.html"><i class="fa-solid fa-chart-line"></i> <span>Dashboard</span></a>
            </li>
            <li class="mobile-drawer-item ${path.endsWith('/review.html') ? 'active' : ''}">
                <a href="/review.html"><i class="fa-solid fa-file-code"></i> <span>Review Code</span></a>
            </li>
            <li class="mobile-drawer-item ${path.endsWith('/history.html') ? 'active' : ''}">
                <a href="/history.html"><i class="fa-solid fa-clock-rotate-left"></i> <span>Review History</span></a>
            </li>
            <li class="mobile-drawer-item ${path.endsWith('/memory.html') ? 'active' : ''}">
                <a href="/memory.html"><i class="fa-solid fa-brain"></i> <span>Hindsight Memory</span></a>
            </li>
            <li class="mobile-drawer-item ${path.endsWith('/settings.html') ? 'active' : ''}">
                <a href="/settings.html"><i class="fa-solid fa-gear"></i> <span>Settings</span></a>
            </li>
        </ul>

        <div class="mobile-drawer-footer">
            ${isAuthenticated ? `
                <button class="btn btn-secondary" onclick="logout()" style="width: 100%; justify-content: center; padding: 0.6rem;">
                    <i class="fa-solid fa-right-from-bracket"></i> Logout
                </button>
            ` : `
                <div style="display: flex; gap: 0.5rem;">
                    <a href="/login.html" class="btn btn-secondary" style="flex: 1; justify-content: center;">Login</a>
                    <a href="/register.html" class="btn btn-primary" style="flex: 1; justify-content: center;">Register</a>
                </div>
            `}
        </div>
    `;

    const links = drawer.querySelectorAll('a');
    links.forEach(link => {
        link.addEventListener('click', closeMobileDrawer);
    });
}

function toggleMobileDrawer() {
    const drawer = document.getElementById('mobile-nav-drawer');
    if (drawer && drawer.classList.contains('active')) {
        closeMobileDrawer();
    } else {
        openMobileDrawer();
    }
}

function openMobileDrawer() {
    const drawer = document.getElementById('mobile-nav-drawer');
    const backdrop = document.getElementById('mobile-nav-backdrop');
    if (drawer && backdrop) {
        drawer.classList.add('active');
        backdrop.classList.add('active');
        document.body.style.overflow = 'hidden';
    }
}

function closeMobileDrawer() {
    const drawer = document.getElementById('mobile-nav-drawer');
    const backdrop = document.getElementById('mobile-nav-backdrop');
    if (drawer && backdrop) {
        drawer.classList.remove('active');
        backdrop.classList.remove('active');
        document.body.style.overflow = '';
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
