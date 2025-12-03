(function() {
  if (typeof window === 'undefined' || !window.axios) {
    console.error('Axios is not loaded. Please ensure axios.min.js is included before api.js');
    return;
  }

  const BASE_URL = 'http://localhost:3000/'; // Placeholder for your server's domain/IP
  let activeRequests = 0;
  const requestQueue = [];
  const MAX_DISPLAYED_REQUESTS = 3;

  // --- Request Capsule Management ---
  function createRequestCapsule() {
    let capsule = document.getElementById('request-capsule-container');
    if (!capsule) {
      capsule = document.createElement('div');
      capsule.id = 'request-capsule-container';
      document.body.appendChild(capsule);
    }
    return capsule;
  }

  function addRequestToCapsule(requestId, requestName) {
    const capsuleContainer = createRequestCapsule();
    const requestElement = document.createElement('div');
    requestElement.id = `request-capsule-${requestId}`;
    requestElement.className = 'request-capsule-item loading';
    requestElement.innerHTML = `
      <span class="request-capsule-text">正在加载: ${requestName}</span>
      <span class="request-capsule-progress">0%</span>
      <div class="request-capsule-bar"></div>
    `;
    capsuleContainer.prepend(requestElement); // Add to the top
    
    // Manage display limit
    const currentItems = capsuleContainer.querySelectorAll('.request-capsule-item');
    if (currentItems.length > MAX_DISPLAYED_REQUESTS) {
      currentItems[currentItems.length - 1].style.display = 'none'; // Hide the oldest one
    }
    return requestElement;
  }

  function updateRequestCapsule(requestId, progress) {
    const requestElement = document.getElementById(`request-capsule-${requestId}`);
    if (requestElement) {
      const progressBar = requestElement.querySelector('.request-capsule-bar');
      const progressText = requestElement.querySelector('.request-capsule-progress');
      if (progressBar && progressText) {
        const percentage = Math.round(progress * 100);
        progressBar.style.width = `${percentage}%`;
        progressText.textContent = `${percentage}%`;
      }
    }
  }

  function removeRequestFromCapsule(requestId, status = 'success') {
    const requestElement = document.getElementById(`request-capsule-${requestId}`);
    if (requestElement) {
      requestElement.classList.remove('loading');
      requestElement.classList.add(status);
      const progressBar = requestElement.querySelector('.request-capsule-bar');
      if (progressBar) {
        progressBar.style.width = '100%';
        progressBar.style.backgroundColor = status === 'success' ? 'var(--accent-primary)' : 'var(--error-color, #ef4444)';
      }
      const progressText = requestElement.querySelector('.request-capsule-progress');
      if (progressText) {
        progressText.textContent = status === 'success' ? '完成' : '失败';
      }
      
      setTimeout(() => {
        requestElement.remove();
        // Show the next hidden item if any
        const capsuleContainer = document.getElementById('request-capsule-container');
        if (capsuleContainer) {
          const hiddenItems = capsuleContainer.querySelectorAll('.request-capsule-item[style*="display: none"]');
          if (hiddenItems.length > 0) {
            hiddenItems[hiddenItems.length - 1].style.display = '';
          }
        }
      }, 3000); // Remove after 3 seconds
    }
  }

  // --- Axios Instance ---
  const service = axios.create({
    baseURL: BASE_URL,
    timeout: 10000 // Request timeout
  });

  // Request interceptor
  service.interceptors.request.use(
    config => {
      // Generate a unique ID for the request
      const requestId = `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
      config._requestId = requestId;
      activeRequests++;
      
      const requestName = config.requestName || config.url; // Use custom name or URL
      addRequestToCapsule(requestId, requestName);

      // Monitor upload progress
      if (config.onUploadProgress) {
        const originalOnUploadProgress = config.onUploadProgress;
        config.onUploadProgress = progressEvent => {
          const percentage = progressEvent.total ? (progressEvent.loaded * 1) / progressEvent.total : 0;
          updateRequestCapsule(requestId, percentage);
          originalOnUploadProgress(progressEvent);
        };
      } else {
        config.onUploadProgress = progressEvent => {
            const percentage = progressEvent.total ? (progressEvent.loaded * 1) / progressEvent.total : 0;
            updateRequestCapsule(requestId, percentage);
        };
      }

      return config;
    },
    error => {
      // Handle request error
      console.error('Request Error:', error);
      Promise.reject(error);
    }
  );

  // Response interceptor
  service.interceptors.response.use(
    response => {
      activeRequests--;
      if (response.config._requestId) {
        removeRequestFromCapsule(response.config._requestId, 'success');
      }
      return response;
    },
    error => {
      activeRequests--;
      if (error.config && error.config._requestId) {
        removeRequestFromCapsule(error.config._requestId, 'error');
      }
      console.error('Response Error:', error);
      return Promise.reject(error);
    }
  );

  // Expose the axios instance globally or within a specific namespace
  window.apiService = service;
})();
