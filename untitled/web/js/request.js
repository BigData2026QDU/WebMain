(function() {
  if (typeof window === 'undefined' || !window.apiService) {
    console.error('apiService (Axios wrapper) is not loaded. Please ensure api.js is included before request.js');
    return;
  }

  class Request {
    /**
     * Creates an instance of Request.
     * @param {string} name - A human-readable name for the request, used for display in the monitoring capsule.
     * @param {string} apiPath - The API endpoint path (e.g., '/users', '/data/report').
     */
    constructor(name, apiPath) {
      if (!name || typeof name !== 'string') {
        throw new Error('Request name must be a non-empty string.');
      }
      if (!apiPath || typeof apiPath !== 'string') {
        throw new Error('API path must be a non-empty string.');
      }

      this.name = name;
      this.apiPath = apiPath;
    }

    /**
     * Makes a GET request.
     * @param {object} params - Query parameters.
     * @param {object} config - Axios request config.
     * @returns {Promise<object>} - A promise that resolves with the response data.
     */
    get(params = {}, config = {}) {
      return window.apiService.get(this.apiPath, {
        params,
        requestName: this.name,
        ...config
      });
    }

    /**
     * Makes a POST request.
     * @param {object} data - Request body data.
     * @param {object} config - Axios request config.
     * @returns {Promise<object>} - A promise that resolves with the response data.
     */
    post(data = {}, config = {}) {
      return window.apiService.post(this.apiPath, data, {
        requestName: this.name,
        ...config
      });
    }

    /**
     * Makes a PUT request.
     * @param {object} data - Request body data.
     * @param {object} config - Axios request config.
     * @returns {Promise<object>} - A promise that resolves with the response data.
     */
    put(data = {}, config = {}) {
      return window.apiService.put(this.apiPath, data, {
        requestName: this.name,
        ...config
      });
    }

    /**
     * Makes a DELETE request.
     * @param {object} config - Axios request config.
     * @returns {Promise<object>} - A promise that resolves with the response data.
     */
    delete(config = {}) {
      return window.apiService.delete(this.apiPath, {
        requestName: this.name,
        ...config
      });
    }

    // You can add more HTTP methods (e.g., patch) as needed
  }

  // Expose the Request class globally
  window.Request = Request;
})();
