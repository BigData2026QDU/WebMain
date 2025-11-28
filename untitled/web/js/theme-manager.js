class ThemeManager {
  constructor(config = {}) {
    this.config = {
      dayStartHour: config.dayStartHour || 6,
      nightStartHour: config.nightStartHour || 18,
      storageKey: config.storageKey || 'theme-preference',
      autoUpdateInterval: config.autoUpdateInterval || 60000,
      enableManualOverride: config.enableManualOverride !== false,
      ...config
    };
    
    this.currentTheme = null;
    this.updateTimer = null;
    
    this.init();
  }
  
  init() {
    const savedTheme = this.getSavedTheme();
    
    if (savedTheme && this.config.enableManualOverride) {
      this.setTheme(savedTheme, false);
    } else {
      this.setThemeByTime();
    }
    
    if (this.config.autoUpdateInterval > 0) {
      this.startAutoUpdate();
    }
    
    this.dispatchEvent('initialized', { theme: this.currentTheme });
  }
  
  getThemeByTime() {
    const now = new Date();
    const hour = now.getHours();
    
    if (hour >= this.config.dayStartHour && hour < this.config.nightStartHour) {
      return 'day';
    } else {
      return 'night';
    }
  }
  
  setThemeByTime() {
    const theme = this.getThemeByTime();
    this.setTheme(theme, false);
  }
  
  setTheme(theme, savePreference = true) {
    if (this.currentTheme === theme) {
      return;
    }
    
    const oldTheme = this.currentTheme;
    this.currentTheme = theme;
    
    if (theme === 'night') {
      document.documentElement.setAttribute('data-theme', 'night');
    } else {
      document.documentElement.removeAttribute('data-theme');
    }
    
    if (savePreference && this.config.enableManualOverride) {
      this.saveTheme(theme);
    }
    
    this.dispatchEvent('themeChanged', {
      oldTheme,
      newTheme: theme,
      isManual: savePreference
    });
  }
  
  toggleTheme() {
    const newTheme = this.currentTheme === 'day' ? 'night' : 'day';
    this.setTheme(newTheme, true);
  }
  
  saveTheme(theme) {
    try {
      localStorage.setItem(this.config.storageKey, theme);
    } catch (e) {
      console.warn('Failed to save theme preference:', e);
    }
  }
  
  getSavedTheme() {
    try {
      return localStorage.getItem(this.config.storageKey);
    } catch (e) {
      console.warn('Failed to retrieve theme preference:', e);
      return null;
    }
  }
  
  clearSavedTheme() {
    try {
      localStorage.removeItem(this.config.storageKey);
      this.setThemeByTime();
    } catch (e) {
      console.warn('Failed to clear theme preference:', e);
    }
  }
  
  startAutoUpdate() {
    this.stopAutoUpdate();
    
    this.updateTimer = setInterval(() => {
      const savedTheme = this.getSavedTheme();
      if (!savedTheme) {
        this.setThemeByTime();
      }
    }, this.config.autoUpdateInterval);
  }
  
  stopAutoUpdate() {
    if (this.updateTimer) {
      clearInterval(this.updateTimer);
      this.updateTimer = null;
    }
  }
  
  dispatchEvent(eventName, detail = {}) {
    const event = new CustomEvent(`theme:${eventName}`, {
      detail: {
        ...detail,
        timestamp: new Date().toISOString()
      }
    });
    document.dispatchEvent(event);
  }
  
  getCurrentTheme() {
    return this.currentTheme;
  }
  
  getConfig() {
    return { ...this.config };
  }
  
  destroy() {
    this.stopAutoUpdate();
    this.dispatchEvent('destroyed');
  }
}

(function() {
  if (typeof window === 'undefined') return;
  
  window.ThemeManager = ThemeManager;
  
  function getConfigFromScript() {
    const script = document.currentScript;
    if (!script) return ;
    
    const config = {};
    
    if (script.dataset.dayStart) config.dayStartHour = parseInt(script.dataset.dayStart);
    if (script.dataset.nightStart) config.nightStartHour = parseInt(script.dataset.nightStart);
    if (script.dataset.interval) config.autoUpdateInterval = parseInt(script.dataset.interval);
    if (script.dataset.storageKey) config.storageKey = script.dataset.storageKey;
    if (script.dataset.manualOverride) config.enableManualOverride = script.dataset.manualOverride !== 'false';
    if (script.dataset.autoInit) config.autoInit = script.dataset.autoInit !== 'false';
    if (script.dataset.autoButton) config.autoButton = script.dataset.autoButton !== 'false';
    if (script.dataset.buttonSelector) config.buttonSelector = script.dataset.buttonSelector;
    
    return config;
  }
  
  function createToggleButton(themeManager) {
    const button = document.createElement('button');
    button.className = 'theme-toggle-auto';
    button.setAttribute('aria-label', '切换主题');
    button.style.cssText = `
      position: fixed;
      top: 1rem;
      right: 1rem;
      z-index: 9999;
      padding: 0.75rem 1.25rem;
      border: none;
      border-radius: 6px;
      background-color: var(--accent-primary, #3b82f6);
      color: white;
      cursor: pointer;
      font-size: 1rem;
      font-weight: 500;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      transition: all 0.2s ease;
    `;
    
    const icon = document.createElement('span');
    icon.style.marginRight = '0.5rem';
    
    const text = document.createElement('span');
    
    button.appendChild(icon);
    button.appendChild(text);
    
    function updateButton(theme) {
      if (theme === 'night') {
        icon.textContent = '☀️';
        text.textContent = '日间';
      } else {
        icon.textContent = '🌙';
        text.textContent = '夜间';
      }
    }
    
    updateButton(themeManager.getCurrentTheme());
    
    button.addEventListener('click', () => {
      themeManager.toggleTheme();
    });
    
    button.addEventListener('mouseenter', () => {
      button.style.transform = 'translateY(-2px)';
      button.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.2)';
    });
    
    button.addEventListener('mouseleave', () => {
      button.style.transform = 'translateY(0)';
      button.style.boxShadow = '0 2px 8px rgba(0, 0, 0, 0.15)';
    });
    
    document.addEventListener('theme:themeChanged', (e) => {
      updateButton(e.detail.newTheme);
    });
    
    return button;
  }
  
  function autoInit() {
    const config = getConfigFromScript();
    
    if (config.autoInit === false) {
      return;
    }
    
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', () => {
        initThemeSystem(config);
      });
    } else {
      initThemeSystem(config);
    }
  }
  
  function initThemeSystem(config) {
    const themeManager = new ThemeManager(config);
    window.themeManager = themeManager;
    
    if (config.autoButton !== false) {
      const customButton = config.buttonSelector 
        ? document.querySelector(config.buttonSelector)
        : null;
      
      if (customButton) {
        customButton.addEventListener('click', () => {
          themeManager.toggleTheme();
        });
        
        document.addEventListener('theme:themeChanged', (e) => {
          if (customButton.dataset) {
            customButton.dataset.theme = e.detail.newTheme;
          }
        });
      } else {
        const button = createToggleButton(themeManager);
        document.body.appendChild(button);
      }
    }
  }
  
  autoInit();
})();
