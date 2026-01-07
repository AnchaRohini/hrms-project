/* offer-auto-view.js */
(function () {
    const offerId = /*[[${offerId}]]*/ null;   // Thymeleaf replaces this
    if (!offerId) return;                       // safety – no id, no action

    // 1.  open the PDF inline in a new tab
    const pdfUrl = '/dashboard/view-offer/' + offerId;
    window.open(pdfUrl, '_blank');

    // 2.  tiny toast in the original tab
    const toast = document.createElement('div');
    toast.style.cssText = `
        position:fixed; top:20px; right:20px; z-index:9999;
        background:#10b981; color:#fff; padding:1rem 1.5rem;
        border-radius:6px; font-size:0.9rem; box-shadow:0 4px 12px rgba(0,0,0,.3);
        animation:slideInRight .3s ease-out`;
    toast.innerHTML = '<i class="fas fa-check-circle me-2"></i>Offer letter generated – downloading in new tab …';
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
})();