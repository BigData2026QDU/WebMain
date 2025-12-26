/**
 * 博客编辑器
 * 基于 Request 类和现有 API 模块
 */
(function() {
  'use strict';

  if (!window.Request) {
    console.error('BlogEditor 依赖 Request 类，请先加载 request.js');
    return;
  }

  class BlogEditor {
    constructor() {
      this.blocks = [];
      this.tableNames = [];
      this.currentBindex = null;
      this.mode = 'new'; // 'new' or 'existing'

      this.init();
    }

    async init() {
      // 加载表名列表
      await this.loadTableNames();

      // 绑定事件
      this.bindEvents();
    }

    async loadTableNames() {
      try {
        const req = new Request('获取数据库表名', 'api/database/tables');
        const resp = await req.get();
        this.tableNames = resp.data.data || [];
        console.log('已加载表名:', this.tableNames.length);
      } catch (error) {
        console.error('加载表名失败:', error);
        this.showMessage('加载表名失败', 'error');
      }
    }

    bindEvents() {
      // 模式切换
      const modeSelect = document.getElementById('bindexMode');
      const bindexInput = document.getElementById('bindexInput');
      const listSelector = document.getElementById('reportListSelector');
      const loadBtn = document.getElementById('loadBtn');
      const refreshListBtn = document.getElementById('refreshListBtn');

      modeSelect.addEventListener('change', (e) => {
        this.mode = e.target.value;
        if (this.mode === 'new') {
          bindexInput.disabled = false;
          listSelector.style.display = 'none';
          refreshListBtn.style.display = 'none';
          loadBtn.disabled = false;
          loadBtn.textContent = '开始新建';
        } else {
          bindexInput.disabled = true;
          listSelector.style.display = 'inline-block';
          refreshListBtn.style.display = 'inline-block';
          loadBtn.disabled = false;
          loadBtn.textContent = '加载报告';
          this.loadReportList();
        }
      });

      // 加载/新建按钮
      loadBtn.addEventListener('click', () => {
        if (this.mode === 'new') {
          const bindex = parseInt(bindexInput.value);
          if (bindex > 0) {
            this.currentBindex = bindex;
            this.blocks = [];
            this.renderBlocks();
            this.showMessage(`已创建新报告 ${bindex}，请添加内容块`, 'success');
          }
        } else {
          const bindex = parseInt(listSelector.value);
          if (bindex > 0) {
            this.loadReport(bindex);
          }
        }
      });

      // 刷新列表
      refreshListBtn.addEventListener('click', () => this.loadReportList());

      // 添加块
      document.getElementById('addTextBtn').addEventListener('click', () => this.addBlock('text'));
      document.getElementById('addChartBtn').addEventListener('click', () => this.addBlock('chart'));

      // 保存/删除/预览
      document.getElementById('saveBtn').addEventListener('click', () => this.saveReport());
      document.getElementById('deleteBtn').addEventListener('click', () => this.deleteReport());
      document.getElementById('previewBtn').addEventListener('click', () => this.previewReport());

      // 关闭预览
      document.getElementById('closePreview').addEventListener('click', () => {
        document.getElementById('previewModal').classList.remove('active');
      });
    }

    async loadReportList() {
      try {
        const req = new Request('获取报告列表', 'api/blog/editor/list');
        const resp = await req.get({ limit: 100 });
        const list = resp.data.data || [];

        const selector = document.getElementById('reportListSelector');
        selector.innerHTML = '<option value="">选择报告...</option>';
        list.forEach(item => {
          const option = document.createElement('option');
          option.value = item.bindex;
          const preview = item.firstText ? item.firstText.substring(0, 30) : '(无文本)';
          option.textContent = `报告 ${item.bindex} - ${item.blocks} 块 - ${preview}`;
          selector.appendChild(option);
        });

      } catch (error) {
        console.error('加载报告列表失败:', error);
        this.showMessage('加载报告列表失败', 'error');
      }
    }

    async loadReport(bindex) {
      try {
        const req = new Request('加载报告', 'api/blog/editor/detail');
        const resp = await req.get({ bindex });
        const detail = resp.data.data;

        this.currentBindex = detail.bindex;

        // 规范化 blocks：添加 _id，规范化 type
        this.blocks = (detail.blocks || []).map(b => ({
          ...b,
          type: (b.type || 'text').toString().trim().toLowerCase(), // 规范化 type
          content: b.content || '',
          _id: Date.now() + Math.random() // 添加临时ID
        }));

        this.renderBlocks();
        this.showMessage(`已加载报告 ${bindex}，共 ${this.blocks.length} 个内容块`, 'success');

      } catch (error) {
        console.error('加载报告失败:', error);
        this.showMessage('加载报告失败', 'error');
      }
    }

    addBlock(type) {
      const block = {
        type: type,
        content: '',
        _id: Date.now() + Math.random() // 临时ID用于DOM操作
      };
      this.blocks.push(block);
      this.renderBlocks();
    }

    removeBlock(blockId) {
      this.blocks = this.blocks.filter(b => b._id !== blockId);
      this.renderBlocks();
    }

    moveBlock(blockId, direction) {
      const idx = this.blocks.findIndex(b => b._id === blockId);
      if (idx === -1) return;

      if (direction === 'up' && idx > 0) {
        [this.blocks[idx], this.blocks[idx - 1]] = [this.blocks[idx - 1], this.blocks[idx]];
      } else if (direction === 'down' && idx < this.blocks.length - 1) {
        [this.blocks[idx], this.blocks[idx + 1]] = [this.blocks[idx + 1], this.blocks[idx]];
      }

      this.renderBlocks();
    }

    renderBlocks() {
      const container = document.getElementById('blocksContainer');
      container.innerHTML = '';

      if (this.blocks.length === 0) {
        container.innerHTML = '<div class="empty-state">暂无内容块，点击下方按钮添加</div>';
        return;
      }

      this.blocks.forEach((block, idx) => {
        const blockEl = this.createBlockElement(block, idx);
        container.appendChild(blockEl);
      });
    }

    createBlockElement(block, index) {
      const blockEl = document.createElement('div');
      blockEl.className = 'block';

      const header = document.createElement('div');
      header.className = 'block-header';

      const badge = document.createElement('span');
      badge.className = `block-type-badge ${block.type}`;
      badge.textContent = block.type === 'text' ? '文本' : '图表';

      const controls = document.createElement('div');
      controls.className = 'block-controls';

      const upBtn = document.createElement('button');
      upBtn.textContent = '↑';
      upBtn.disabled = index === 0;
      upBtn.addEventListener('click', () => this.moveBlock(block._id, 'up'));

      const downBtn = document.createElement('button');
      downBtn.textContent = '↓';
      downBtn.disabled = index === this.blocks.length - 1;
      downBtn.addEventListener('click', () => this.moveBlock(block._id, 'down'));

      const deleteBtn = document.createElement('button');
      deleteBtn.textContent = '×';
      deleteBtn.addEventListener('click', () => {
        if (confirm('确定删除此内容块？')) {
          this.removeBlock(block._id);
        }
      });

      controls.appendChild(upBtn);
      controls.appendChild(downBtn);
      controls.appendChild(deleteBtn);

      header.appendChild(badge);
      header.appendChild(controls);
      blockEl.appendChild(header);

      const content = document.createElement('div');
      content.className = 'block-content';

      if (block.type === 'text') {
        const textarea = document.createElement('textarea');
        textarea.value = block.content || '';
        textarea.placeholder = '输入文本内容...';
        textarea.addEventListener('input', (e) => {
          block.content = e.target.value;
        });
        content.appendChild(textarea);
      } else if (block.type === 'chart') {
        const selector = this.createChartSelector(block);
        content.appendChild(selector);
      }

      blockEl.appendChild(content);
      return blockEl;
    }

    createChartSelector(block) {
      const wrapper = document.createElement('div');
      wrapper.className = 'chart-selector';

      const label = document.createElement('label');
      label.textContent = '选择表名:';

      const select = document.createElement('select');
      select.innerHTML = '<option value="">请选择...</option>';
      this.tableNames.forEach(table => {
        const option = document.createElement('option');
        option.value = table;
        option.textContent = table;
        if (block.content === table) {
          option.selected = true;
        }
        select.appendChild(option);
      });

      select.addEventListener('change', (e) => {
        block.content = e.target.value;
      });

      const previewBtn = document.createElement('button');
      previewBtn.className = 'btn btn-secondary';
      previewBtn.textContent = '预览数据';
      previewBtn.addEventListener('click', () => this.previewTableData(block.content, wrapper));

      wrapper.appendChild(label);
      wrapper.appendChild(select);
      wrapper.appendChild(previewBtn);

      return wrapper;
    }

    async previewTableData(tableName, containerEl) {
      if (!tableName) {
        alert('请先选择表名');
        return;
      }

      try {
        const req = new Request('预览表数据', 'api/database/preview');
        const resp = await req.get({ table: tableName, limit: 5 });
        const data = resp.data.data;

        // 移除旧预览
        const oldPreview = containerEl.querySelector('.table-preview');
        if (oldPreview) oldPreview.remove();

        // 创建新预览
        const preview = document.createElement('div');
        preview.className = 'table-preview';

        const table = document.createElement('table');
        const thead = document.createElement('thead');
        const tr = document.createElement('tr');
        data.columns.forEach(col => {
          const th = document.createElement('th');
          th.textContent = col;
          tr.appendChild(th);
        });
        thead.appendChild(tr);
        table.appendChild(thead);

        const tbody = document.createElement('tbody');
        data.rows.forEach(row => {
          const tr = document.createElement('tr');
          row.forEach(cell => {
            const td = document.createElement('td');
            td.textContent = cell != null ? cell : 'NULL';
            tr.appendChild(td);
          });
          tbody.appendChild(tr);
        });
        table.appendChild(tbody);

        preview.appendChild(table);
        containerEl.appendChild(preview);

      } catch (error) {
        console.error('预览数据失败:', error);
        alert('预览数据失败');
      }
    }

    async saveReport() {
      if (!this.currentBindex) {
        alert('请先选择或新建报告');
        return;
      }

      // 过滤空内容块
      const blocks = this.blocks
        .filter(b => b.content && b.content.trim())
        .map(b => ({ type: b.type, content: b.content }));

      if (blocks.length === 0) {
        alert('至少需要一个非空内容块');
        return;
      }

      try {
        const req = new Request('保存报告', 'api/blog/editor/save');
        const resp = await req.post({
          bindex: this.currentBindex,
          blocks: blocks
        });

        this.showMessage(`报告 ${this.currentBindex} 保存成功，共 ${blocks.length} 个内容块`, 'success');
      } catch (error) {
        console.error('保存失败:', error);
        this.showMessage('保存失败', 'error');
      }
    }

    async deleteReport() {
      if (!this.currentBindex) {
        alert('请先选择报告');
        return;
      }

      if (!confirm(`确定删除报告 ${this.currentBindex}？`)) {
        return;
      }

      try {
        const req = new Request('删除报告', 'api/blog/editor/delete');
        const resp = await req.delete({ params: { bindex: this.currentBindex } });

        this.showMessage(`报告 ${this.currentBindex} 已删除`, 'success');
        this.blocks = [];
        this.currentBindex = null;
        this.renderBlocks();

        if (this.mode === 'existing') {
          this.loadReportList();
        }
      } catch (error) {
        console.error('删除失败:', error);
        this.showMessage('删除失败', 'error');
      }
    }

    async previewReport() {
      if (!this.currentBindex) {
        alert('请先加载或创建报告');
        return;
      }

      try {
        const req = new Request('加载报告预览', `api/report-test`);
        const resp = await req.get({ bindex: this.currentBindex });
        const report = resp.data;

        // 显示预览模态框
        const modal = document.getElementById('previewModal');
        const viewer = document.getElementById('previewViewer');
        viewer.innerHTML = '';

        // 使用 ReportRenderer 渲染
        if (window.ReportRenderer) {
          const renderer = new window.ReportRenderer({ container: viewer });
          renderer.renderFromPayload(report);
        } else {
          viewer.textContent = JSON.stringify(report, null, 2);
        }

        modal.classList.add('active');

      } catch (error) {
        console.error('预览失败:', error);
        alert('预览失败，请确保报告已保存');
      }
    }

    showMessage(text, type = 'success') {
      const msgEl = document.getElementById('statusMessage');
      msgEl.className = `status-message ${type}`;
      msgEl.textContent = text;
      msgEl.style.display = 'block';

      setTimeout(() => {
        msgEl.style.display = 'none';
      }, 5000);
    }
  }

  // 初始化
  document.addEventListener('DOMContentLoaded', () => {
    window.blogEditor = new BlogEditor();
  });

})();
