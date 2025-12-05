(function() {
  if (typeof window === 'undefined' || !window.Request) {
    console.error('ReportRenderer 依赖 Request 封装，请确认 request.js 已加载。');
    return;
  }

  class ReportRenderer {
    constructor(options = {}) {
      this.container = options.container;
      this.statusElement = options.statusElement || null;
      this.requestFactory = options.requestFactory || ((reportId) => new Request(`加载报告:${reportId}`, `reports/${reportId}`));

      if (!this.container) {
        throw new Error('ReportRenderer 需要传入 container DOM 节点。');
      }
    }

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

    decodePayload(payload) {
      if (!payload) {
        throw new Error('报告数据为空，无法解码。');
      }

      // 支持 base64 编码的 JSON 或直接传入的对象
      if (payload.base64) {
        try {
          const jsonString = atob(payload.base64);
          return JSON.parse(jsonString);
        } catch (e) {
          throw new Error('报告解码失败：base64 内容无法解析为 JSON。');
        }
      }

      if (payload.report) {
        return payload.report;
      }

      if (typeof payload === 'object') {
        return payload;
      }

      throw new Error('报告数据格式不受支持。');
    }

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

    renderFromPayload(payload) {
      try {
        const decoded = this.decodePayload(payload);
        this.renderReport(decoded);
        this.setStatus('ready', '已加载示例报告');
      } catch (err) {
        console.error(err);
        this.renderError(err);
        this.setStatus('error', '示例加载失败');
      }
    }

    renderReport(report) {
      if (!report) {
        this.renderEmpty();
        return;
      }

      this.container.innerHTML = '';

      const wrapper = document.createElement('div');
      wrapper.className = 'report-body';

      // 头部
      const header = document.createElement('div');
      header.className = 'report-section';
      header.appendChild(this.buildHeader(report));
      wrapper.appendChild(header);

      // 关键指标
      if (Array.isArray(report.metrics) && report.metrics.length > 0) {
        const metricsSection = document.createElement('div');
        metricsSection.className = 'report-section';
        const metricsTitle = document.createElement('h3');
        metricsTitle.textContent = '关键指标';
        metricsSection.appendChild(metricsTitle);

        const metricGrid = document.createElement('div');
        metricGrid.className = 'metric-grid';
        report.metrics.forEach(metric => {
          metricGrid.appendChild(this.buildMetricCard(metric));
        });
        metricsSection.appendChild(metricGrid);
        wrapper.appendChild(metricsSection);
      }

      // 文本段落
      if (Array.isArray(report.sections) && report.sections.length > 0) {
        report.sections.forEach(section => {
          const block = document.createElement('div');
          block.className = 'report-section';
          const title = document.createElement('h3');
          title.textContent = section.title || '内容';
          block.appendChild(title);

          (section.paragraphs || []).forEach(text => {
            const p = document.createElement('p');
            p.textContent = text;
            block.appendChild(p);
          });

          if (Array.isArray(section.list) && section.list.length > 0) {
            const ul = document.createElement('ul');
            section.list.forEach(item => {
              const li = document.createElement('li');
              li.textContent = item;
              ul.appendChild(li);
            });
            block.appendChild(ul);
          }

          wrapper.appendChild(block);
        });
      }

      // 图表（简单柱状图示意）
      if (Array.isArray(report.charts) && report.charts.length > 0) {
        const chartSection = document.createElement('div');
        chartSection.className = 'report-section';
        const ct = document.createElement('h3');
        ct.textContent = '可视化';
        chartSection.appendChild(ct);

        report.charts.forEach(chart => {
          chartSection.appendChild(this.buildChart(chart));
        });

        wrapper.appendChild(chartSection);
      }

      // 附注
      if (Array.isArray(report.notes) && report.notes.length > 0) {
        const notes = document.createElement('div');
        notes.className = 'report-section';
        const title = document.createElement('h3');
        title.textContent = '附注';
        notes.appendChild(title);
        report.notes.forEach(text => {
          const p = document.createElement('p');
          p.textContent = text;
          notes.appendChild(p);
        });
        wrapper.appendChild(notes);
      }

      this.container.appendChild(wrapper);
    }

    buildHeader(report) {
      const header = document.createElement('div');

      const headerTop = document.createElement('div');
      headerTop.className = 'report-header';

      const title = document.createElement('h2');
      title.className = 'report-title';
      title.textContent = report.title || '未命名报告';
      headerTop.appendChild(title);

      const badge = document.createElement('div');
      badge.className = 'report-badge';
      badge.textContent = report.tag || '在线报告';
      headerTop.appendChild(badge);

      header.appendChild(headerTop);

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

      if (report.summary) {
        const summary = document.createElement('p');
        summary.textContent = report.summary;
        summary.style.marginTop = '0.5rem';
        summary.style.color = 'var(--text-secondary)';
        header.appendChild(meta);
        header.appendChild(summary);
      } else {
        header.appendChild(meta);
      }

      return header;
    }

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
        note.style.marginTop = '0.35rem';
        card.appendChild(note);
      }

      return card;
    }

    buildChart(chart) {
      const chartBlock = document.createElement('div');
      chartBlock.className = 'chart-block';

      const title = document.createElement('div');
      title.className = 'chart-title';
      title.textContent = chart.title || '图表';
      chartBlock.appendChild(title);

      const bars = document.createElement('div');
      bars.className = 'chart-bars';

      const maxValue = Math.max(...(chart.data || []).map(item => item.value || 0), 1);

      (chart.data || []).forEach(item => {
        const row = document.createElement('div');
        row.className = 'chart-row';

        const label = document.createElement('div');
        label.className = 'chart-label';
        label.textContent = item.label || '项';
        row.appendChild(label);

        const track = document.createElement('div');
        track.className = 'chart-bar-track';

        const fill = document.createElement('div');
        fill.className = 'chart-bar-fill';
        const width = Math.min(100, Math.round(((item.value || 0) / maxValue) * 100));
        fill.style.width = `${width}%`;
        track.appendChild(fill);

        row.appendChild(track);

        const value = document.createElement('div');
        value.className = 'chart-value';
        value.textContent = item.value != null ? item.value : '-';
        row.appendChild(value);

        bars.appendChild(row);
      });

      chartBlock.appendChild(bars);
      return chartBlock;
    }

    renderEmpty() {
      this.container.innerHTML = '';
      const empty = document.createElement('div');
      empty.className = 'empty-state';
      empty.textContent = '暂无报告数据，请先加载。';
      this.container.appendChild(empty);
    }

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

  window.ReportRenderer = ReportRenderer;
})();
