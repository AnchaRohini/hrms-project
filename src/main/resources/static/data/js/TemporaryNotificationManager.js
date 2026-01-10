// TemporaryNotificationManager.js
class TemporaryNotificationManager {
    constructor() {
        this.tempNotifications = new Map(); // Store temporary notifications
        this.loadFromStorage();
    }

    addTemporaryNotification(notification) {
        const id = Date.now() + Math.random();
        const tempNotification = {
            id: id,
            type: notification.type,
            title: notification.title,
            message: notification.message,
            createdAt: new Date().toISOString(),
            read: false,
            persistent: false, // Mark as temporary
            color: this.getColorForType(notification.type),
            icon: this.getIconForType(notification.type)
        };

        this.tempNotifications.set(id, tempNotification);
        this.saveToStorage();
        return tempNotification;
    }

    getUnreadCount() {
        return Array.from(this.tempNotifications.values())
            .filter(n => !n.read).length;
    }

    getTemporaryNotifications() {
        return Array.from(this.tempNotifications.values())
            .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    }

    markAsRead(id) {
        const notification = this.tempNotifications.get(id);
        if (notification) {
            notification.read = true;
            this.saveToStorage();
        }
    }

    markAllAsRead() {
        this.tempNotifications.forEach(n => n.read = true);
        this.saveToStorage();
    }

    cleanupOld() {
        const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
        for (const [id, notification] of this.tempNotifications) {
            const created = new Date(notification.createdAt).getTime();
            if (created < oneDayAgo || notification.read) {
                this.tempNotifications.delete(id);
            }
        }
        this.saveToStorage();
    }

    loadFromStorage() {
        try {
            const saved = localStorage.getItem('tempNotifications');
            if (saved) {
                const data = JSON.parse(saved);
                this.tempNotifications = new Map(data);
            }
        } catch (e) {
            console.error('Failed to load temp notifications:', e);
        }
    }

    saveToStorage() {
        try {
            localStorage.setItem('tempNotifications',
                JSON.stringify(Array.from(this.tempNotifications.entries())));
        } catch (e) {
            console.error('Failed to save temp notifications:', e);
        }
    }

    getColorForType(type) {
        if (type.includes('VERIFIED')) return 'success';
        if (type.includes('REJECTED')) return 'danger';
        if (type.includes('WARNING')) return 'warning';
        if (type.includes('UPLOADED')) return 'info';
        return 'primary';
    }

    getIconForType(type) {
        if (type.includes('VERIFIED')) return 'fa-check-circle';
        if (type.includes('REJECTED')) return 'fa-times-circle';
        if (type.includes('WARNING')) return 'fa-clock';
        if (type.includes('UPLOADED')) return 'fa-file-upload';
        return 'fa-bell';
    }
}