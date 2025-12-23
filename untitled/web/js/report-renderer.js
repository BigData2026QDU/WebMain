/**
 * ReportRenderer - 报告渲染器
 * 用于渲染包含文本、指标、图表的完整报告
 * 版本: 3.0 - 集成新图表渲染系统
 */
(function() {
  'use strict';

  // 依赖检查
  if (typeof window === 'undefined' || !window.Request) {
    console.error('ReportRenderer 依赖 Request 封装，请确认 request.js 已加载。');
    return;
  }
  if (!window.echarts) {
    console.error('ReportRenderer 依赖 ECharts，请在 report-renderer.js 之前引入 echarts.min.js。');
  }
  if (!window.ChartParser) {
    console.error('ReportRenderer 依赖 ChartParser，请确认 chart-parser.js 已加载。');
    return;
  }
  if (!window.ChartFactory) {
    console.error('ReportRenderer 依赖 ChartFactory，请确认 chart-factory.js 已加载。');
    return;
  }

  class ReportRenderer {
    constructor(options = {}) {
      this.container = options.container;
      this.statusElement = options.statusElement || null;
      this.requestFactory = options.requestFactory || ((reportId) => new Request(`加载报告:${reportId}`, `reports/${reportId}`));
      this.chartInstances = [];

      if (!this.container) {
        throw new Error('ReportRenderer 需要传入 container DOM 节点。');
      }

      // 监听主题变化
      document.addEventListener('theme:themeChanged', () => {
        this.refreshCharts();
      });
    }

    /**
     * 设置状态显示
     */
    setStatus(type, message) {
      if (!this.statusElement) return;
      const dot = this.statusElement.querySelector('.status-dot');
      const text = this.statusElement.querySelector('.status-text');
      if (dot) {
        dot.classList.toggle('error', type === 'error');
      }
      if (text) {
        text.textContent = message || '';
      }
    }

    /**
     * 显示加载骨架屏
     */
    showSkeleton() {
      const skeleton = document.createElement('div');
      skeleton.className = 'report-section';
      skeleton.innerHTML = `
        <div class="report-header">
          <div class="report-title skeleton" style="width: 180px; height: 20px;"></div>
          <div class="report-badge skeleton" style="width: 120px; height: 20px;"></div>
        </div>
        <div class="report-grid">
          <div class="report-card skeleton" style="height: 90px;"></div>
          <div class="report-card skeleton" style="height: 90px;"></div>
          <div class="report-card skeleton" style="height: 90px;"></div>
        </div>
      `;
      this.container.innerHTML = '';
      this.container.appendChild(skeleton);
    }

    /**
     * 解码报告数据（支持base64或直接对象）
     */
    decodePayload(payload) {
      if (!payload) {
        throw new Error('报告数据为空，无法解码。');
      }

      // 支持 base64 编码的 JSON
      if (payload.base64) {
        try {
          const jsonString = atob(payload.base64);
          return JSON.parse(jsonString);
        } catch (e) {
          throw new Error('报告解码失败：base64 内容无法解析为 JSON。');
        }
      }

      // 支持直接传入报告对象
      if (payload.report) {
        return payload.report;
      }

      if (typeof payload === 'object') {
        return payload;
      }

      throw new Error('报告数据格式不受支持。');
    }

    /**
     * 从报告ID加载报告
     */
    async load(reportId) {
      if (!reportId) {
        throw new Error('reportId 不能为空。');
      }
      this.setStatus('loading', `正在加载报告：${reportId}`);
      this.showSkeleton();

      try {
        const request = this.requestFactory(reportId);
        const response = await request.get({}, { responseType: 'json' });
        const decoded = this.decodePayload(response.data);
        this.renderReport(decoded);
        this.setStatus('ready', `报告 ${reportId} 加载完成`);
      } catch (err) {
        console.error(err);
        this.renderError(err);
        this.setStatus('error', '加载失败，请检查网络或数据格式');
      }
    }

    /**
     * 从数据对象直接渲染报告
     */
    renderFromPayload(payload) {
      try {
        const decoded = this.decodePayload(payload);
        this.renderReport(decoded);
        this.setStatus('ready', '报告已加载');
      } catch (err) {
        console.error(err);
        this.renderError(err);
        this.setStatus('error', '报告加载失败');
      }
    }

    /**
     * 渲染完整报告
     */
    renderReport(report) {
      if (!report) {
        this.renderEmpty();
        return;
      }

      this.disposeCharts();
      this.container.innerHTML = '';

      const wrapper = document.createElement('div');
      wrapper.className = 'report-body';

      // 1. 头部（标题、元数据、摘要）
      const header = document.createElement('div');
      header.className = 'report-section';
      header.appendChild(this.buildHeader(report));
      wrapper.appendChild(header);

      // 2. 关键指标
      if (Array.isArray(report.metrics) && report.metrics.length > 0) {
        const metricsSection = document.createElement('div');
        metricsSection.className = 'report-section';
        const metricsTitle = document.createElement('h3');
        metricsTitle.textContent = '关键指标';
        metricsTitle.className = 'section-title';
        metricsSection.appendChild(metricsTitle);

        const metricGrid = document.createElement('div');
        metricGrid.className = 'metric-grid';
        report.metrics.forEach(metric => {
          metricGrid.appendChild(this.buildMetricCard(metric));
        });
        metricsSection.appendChild(metricGrid);
        wrapper.appendChild(metricsSection);
      }

      // 3. 文章内容段落
      if (Array.isArray(report.sections) && report.sections.length > 0) {
        report.sections.forEach(section => {
          wrapper.appendChild(this.buildSection(section));
        });
      }

      // 4. 图表可视化
      if (Array.isArray(report.charts) && report.charts.length > 0) {
        const chartSection = document.createElement('div');
        chartSection.className = 'report-section';
        const ct = document.createElement('h3');
        ct.textContent = '数据可视化';
        ct.className = 'section-title';
        chartSection.appendChild(ct);

        report.charts.forEach(chart => {
          chartSection.appendChild(this.buildChart(chart));
        });

        wrapper.appendChild(chartSection);
      }

      // 5. 附注
      if (Array.isArray(report.notes) && report.notes.length > 0) {
        const notes = document.createElement('div');
        notes.className = 'report-section notes-section';
        const title = document.createElement('h3');
        title.textContent = '附注';
        title.className = 'section-title';
        notes.appendChild(title);
        report.notes.forEach(text => {
          const p = document.createElement('p');
          p.textContent = text;
          p.className = 'note-text';
          notes.appendChild(p);
        });
        wrapper.appendChild(notes);
      }

      this.container.appendChild(wrapper);
    }

    /**
     * 构建报告头部
     */
    buildHeader(report) {
      const header = document.createElement('div');

      // 标题行
      const headerTop = document.createElement('div');
      headerTop.className = 'report-header';

      const title = document.createElement('h2');
      title.className = 'report-title';
      title.textContent = report.title || '未命名报告';
      headerTop.appendChild(title);

      if (report.tag) {
        const badge = document.createElement('div');
        badge.className = 'report-badge';
        badge.textContent = report.tag;
        headerTop.appendChild(badge);
      }

      header.appendChild(headerTop);

      // 元数据行
      const meta = document.createElement('div');
      meta.className = 'report-meta';

      if (report.updatedAt) {
        const m = document.createElement('span');
        const label = document.createElement('strong');
        label.textContent = '更新时间：';
        m.appendChild(label);
        m.appendChild(document.createTextNode(report.updatedAt));
        meta.appendChild(m);
      }

      if (report.source) {
        const s = document.createElement('span');
        const label = document.createElement('strong');
        label.textContent = '数据源：';
        s.appendChild(label);
        s.appendChild(document.createTextNode(report.source));
        meta.appendChild(s);
      }

      if (report.range) {
        const r = document.createElement('span');
        const label = document.createElement('strong');
        label.textContent = '区间：';
        r.appendChild(label);
        r.appendChild(document.createTextNode(report.range));
        meta.appendChild(r);
      }

      header.appendChild(meta);

      // 摘要
      if (report.summary) {
        const summary = document.createElement('p');
        summary.textContent = report.summary;
        summary.className = 'report-summary';
        header.appendChild(summary);
      }

      return header;
    }

    /**
     * 构建指标卡片
     */
    buildMetricCard(metric) {
      const card = document.createElement('div');
      card.className = 'metric-card';

      const label = document.createElement('div');
      label.className = 'metric-label';
      label.textContent = metric.label || '指标';
      card.appendChild(label);

      const value = document.createElement('div');
      value.className = 'metric-value';
      value.textContent = metric.value != null ? metric.value : '-';
      card.appendChild(value);

      if (metric.trend) {
        const trend = document.createElement('span');
        trend.className = 'metric-trend';
        trend.textContent = metric.trend;
        card.appendChild(trend);
      }

      if (metric.note) {
        const note = document.createElement('p');
        note.textContent = metric.note;
        note.className = 'metric-note';
        card.appendChild(note);
      }

      return card;
    }

    /**
     * 构建文章段落（增强版）
     */
    buildSection(section) {
      const block = document.createElement('div');
      block.className = 'report-section content-section';

      // 标题
      if (section.title) {
        const title = document.createElement('h3');
        title.textContent = section.title;
        title.className = 'section-title';
        block.appendChild(title);
      }

      // 段落
      if (Array.isArray(section.paragraphs) && section.paragraphs.length > 0) {
        section.paragraphs.forEach(text => {
          const p = document.createElement('p');
          p.textContent = text;
          p.className = 'section-paragraph';
          block.appendChild(p);
        });
      }

      // 列表
      if (Array.isArray(section.list) && section.list.length > 0) {
        const ul = document.createElement('ul');
        ul.className = 'section-list';
        section.list.forEach(item => {
          const li = document.createElement('li');
          li.textContent = item;
          ul.appendChild(li);
        });
        block.appendChild(ul);
      }

      // 有序列表
      if (Array.isArray(section.orderedList) && section.orderedList.length > 0) {
        const ol = document.createElement('ol');
        ol.className = 'section-list';
        section.orderedList.forEach(item => {
          const li = document.createElement('li');
          li.textContent = item;
          ol.appendChild(li);
        });
        block.appendChild(ol);
      }

      // 代码块
      if (section.code) {
        const pre = document.createElement('pre');
        pre.className = 'section-code';
        const code = document.createElement('code');
        code.textContent = section.code;
        pre.appendChild(code);
        block.appendChild(pre);
      }

      // 引用块
      if (section.quote) {
        const blockquote = document.createElement('blockquote');
        blockquote.className = 'section-quote';
        blockquote.textContent = section.quote;
        block.appendChild(blockquote);
      }

      return block;
    }

    /**
     * 构建图表（支持新旧格式）
     */
    buildChart(chart) {
      const chartBlock = document.createElement('div');
      chartBlock.className = 'chart-block';

      if (chart.title) {
        const title = document.createElement('div');
        title.className = 'chart-title';
        title.textContent = chart.title;
        chartBlock.appendChild(title);
      }

      const canvas = document.createElement('div');
      canvas.className = 'chart-canvas';
      canvas.style.height = chart.height || '400px';
      chartBlock.appendChild(canvas);

      if (window.echarts) {
        try {
          const instance = echarts.init(canvas);
          const option = this.buildChartOption(chart);
          instance.setOption(option);
          this.chartInstances.push({ instance, chart });
        } catch (error) {
          console.error('图表渲染失败:', error);
          const errorMsg = document.createElement('div');
          errorMsg.className = 'empty-state';
          errorMsg.textContent = `图表渲染失败: ${error.message}`;
          chartBlock.appendChild(errorMsg);
        }
      } else {
        const fallback = document.createElement('div');
        fallback.className = 'empty-state';
        fallback.textContent = 'ECharts 未加载，无法渲染图表。';
        chartBlock.appendChild(fallback);
      }

      return chartBlock;
    }

    /**
     * 构建图表配置（支持新旧格式）
     */
    buildChartOption(chart) {
      const colors = this.getThemeColors();

      // 检测是否为新格式：{ chartType, columns, rows, config }
      if (chart.columns && chart.rows) {
        // 使用新的图表渲染系统
        const chartType = chart.chartType || 'bar';
        const config = chart.config || {};

        // 验证数据格式
        const validation = window.ChartParser.validate(chart);
        if (!validation.valid) {
          throw new Error(`图表数据验证失败: ${validation.errors.join(', ')}`);
        }

        // 解析数据
        let parsedData;
        if (chartType === 'pie') {
          parsedData = window.ChartParser.parsePie(chart, config);
        } else if (chartType === 'scatter') {
          parsedData = window.ChartParser.parseScatter(chart, config);
        } else {
          parsedData = window.ChartParser.parse(chart, config);
        }

        // 生成图表配置
        const chartOptions = config.chartOptions || {};
        return window.ChartFactory.create(chartType, parsedData, colors, chartOptions);
      }

      // 兼容旧格式：{ data: [{ label, value }] }
      if (chart.data && Array.isArray(chart.data)) {
        console.warn('使用了旧的图表数据格式，建议迁移到新的 columns/rows 格式');
        return this.buildLegacyChartOption(chart, colors);
      }

      throw new Error('图表数据格式不正确，需要 columns+rows 或 data 格式');
    }

    /**
     * 兼容旧数据格式的图表配置生成
     */
    buildLegacyChartOption(chart, colors) {
      const labels = (chart.data || []).map(item => item.label || '项');
      const values = (chart.data || []).map(item => item.value || 0);

      return {
        backgroundColor: colors.bgPrimary,
        color: colors.seriesColors,
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          textStyle: { color: colors.textPrimary },
          backgroundColor: colors.bgSecondary,
          borderColor: colors.border
        },
        grid: {
          left: 12,
          right: 12,
          top: 24,
          bottom: 24,
          containLabel: true
        },
        xAxis: {
          type: 'value',
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border } }
        },
        yAxis: {
          type: 'category',
          data: labels,
          axisLabel: { color: colors.textPrimary },
          axisLine: { lineStyle: { color: colors.border } },
          axisTick: { show: false }
        },
        series: [
          {
            type: 'bar',
            data: values,
            barWidth: '50%',
            itemStyle: {
              borderRadius: [0, 6, 6, 0]
            },
            label: {
              show: true,
              position: 'right',
              color: colors.textPrimary
            }
          }
        ],
        textStyle: {
          color: colors.textPrimary,
          fontFamily: 'inherit'
        }
      };
    }

    /**
     * 获取主题颜色
     */
    getThemeColors() {
      const style = getComputedStyle(document.documentElement);
      return {
        bgPrimary: style.getPropertyValue('--bg-primary').trim() || '#ffffff',
        bgSecondary: style.getPropertyValue('--bg-secondary').trim() || '#f5f5f5',
        textPrimary: style.getPropertyValue('--text-primary').trim() || '#1a1a1a',
        textSecondary: style.getPropertyValue('--text-secondary').trim() || '#4a4a4a',
        border: style.getPropertyValue('--border-color').trim() || '#d0d0d0',
        accentPrimary: style.getPropertyValue('--accent-primary').trim() || '#3b82f6',
        accentSecondary: style.getPropertyValue('--accent-secondary').trim() || '#60a5fa',
        seriesColors: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']
      };
    }

    /**
     * 刷新所有图表（主题变化时）
     */
    refreshCharts() {
      if (!this.chartInstances.length) return;
      this.chartInstances.forEach(({ instance, chart }) => {
        const option = this.buildChartOption(chart);
        instance.setOption(option, true);
      });
    }

    /**
     * 销毁所有图表实例
     */
    disposeCharts() {
      this.chartInstances.forEach(({ instance }) => {
        if (instance && instance.dispose) {
          instance.dispose();
        }
      });
      this.chartInstances = [];
    }

    /**
     * 渲染空状态
     */
    renderEmpty() {
      this.container.innerHTML = '';
      const empty = document.createElement('div');
      empty.className = 'empty-state';
      empty.textContent = '暂无报告数据，请先加载。';
      this.container.appendChild(empty);
    }

    /**
     * 渲染错误状态
     */
    renderError(error) {
      this.container.innerHTML = '';
      const err = document.createElement('div');
      err.className = 'error-state';

      const title = document.createElement('strong');
      title.textContent = '加载失败：';
      err.appendChild(title);

      const msg = document.createTextNode(error && error.message ? error.message : '未知错误');
      err.appendChild(msg);

      this.container.appendChild(err);
    }
  }

  // 导出到全局
  window.ReportRenderer = ReportRenderer;
})();
