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
      this.tableColumnsCache = new Map(); // tableName -> string[]
      this.supportedChartTypes = [
        { type: 'bar', name: '柱状图' },
        { type: 'line', name: '折线图' },
        { type: 'pie', name: '饼图' },
        { type: 'scatter', name: '散点图' },
        { type: 'mix', name: '混合图' }
      ];
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
      const bindexAuto = document.getElementById('bindexAuto');
      const bindexInput = document.getElementById('bindexInput'); // hidden compatibility
      const listSelector = document.getElementById('reportListSelector');
      const loadBtn = document.getElementById('loadBtn');
      const refreshListBtn = document.getElementById('refreshListBtn');

      if (!modeSelect || !listSelector || !loadBtn || !refreshListBtn) {
        console.error('BlogEditor: 页面DOM不完整，无法初始化事件绑定', {
          modeSelect: !!modeSelect,
          listSelector: !!listSelector,
          loadBtn: !!loadBtn,
          refreshListBtn: !!refreshListBtn
        });
        return;
      }

      const syncModeUI = () => {
        const value = modeSelect.value;
        this.mode = value;
        if (value === 'new') {
          if (bindexAuto) bindexAuto.style.display = 'inline-flex';
          if (bindexInput) bindexInput.disabled = true;
          listSelector.style.display = 'none';
          refreshListBtn.style.display = 'none';
          loadBtn.disabled = false;
          loadBtn.textContent = '开始新建';
          this.prepareNextBindex();
        } else {
          if (bindexAuto) bindexAuto.style.display = 'none';
          if (bindexInput) bindexInput.disabled = true;
          listSelector.style.display = 'inline-block';
          refreshListBtn.style.display = 'inline-block';
          loadBtn.disabled = false;
          loadBtn.textContent = '加载报告';
          this.loadReportList();
        }
      };

      modeSelect.addEventListener('change', (e) => {
        syncModeUI();
      });

      // 加载/新建按钮
      loadBtn.addEventListener('click', async () => {
        if (this.mode === 'new') {
          const bindex = await this.fetchNextBindex();
          if (!bindex) return;
          this.currentBindex = bindex;
          if (bindexInput) bindexInput.value = String(bindex);
          this.blocks = [];
          this.renderBlocks();
          this.showMessage(`已创建新报告 ${bindex}（自动分配），请添加内容块`, 'success');
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

      // 初始化：同步一次 UI 状态（默认 new 会把按钮启用，否则页面看起来“没反应”）
      syncModeUI();
    }

    async prepareNextBindex() {
      const bindexAuto = document.getElementById('bindexAuto');
      const bindexInput = document.getElementById('bindexInput');
      if (bindexAuto) bindexAuto.textContent = '自动分配...';
      try {
        const bindex = await this.fetchNextBindex();
        if (bindex) {
          if (bindexAuto) bindexAuto.textContent = `下一个 bindex：${bindex}`;
          if (bindexInput) bindexInput.value = String(bindex);
        } else {
          if (bindexAuto) bindexAuto.textContent = '自动分配失败';
        }
      } catch (e) {
        if (bindexAuto) bindexAuto.textContent = '自动分配失败';
      }
    }

    async fetchNextBindex() {
      try {
        const req = new Request('获取下一个 bindex', 'api/blog/editor/next');
        const resp = await req.get({ refresh: 1 });
        const data = resp && resp.data ? resp.data.data : null;
        const bindex = data && data.bindex != null ? parseInt(data.bindex) : NaN;
        if (!bindex || Number.isNaN(bindex) || bindex <= 0) {
          this.showMessage('无法获取下一个 bindex', 'error');
          return null;
        }
        return bindex;
      } catch (error) {
        console.error('获取下一个 bindex 失败:', error);

        // 检查是否是权限错误
        if (error.response && error.response.status === 403) {
          this.showMessage('获取 bindex 失败：你没有管理员权限，无法创建新报告', 'error');
        } else if (error.response && error.response.data && error.response.data.message) {
          this.showMessage('获取 bindex 失败：' + error.response.data.message, 'error');
        } else {
          this.showMessage('获取下一个 bindex 失败', 'error');
        }

        return null;
      }
    }

    async loadReportList() {
      try {
        const req = new Request('获取报告列表', 'api/blog/editor/list');
        const resp = await req.get({ limit: 100, refresh: 1 });
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
        this.blocks = (detail.blocks || []).map(b => {
          const type = (b.type || 'text').toString().trim().toLowerCase();
          const content = b.content || '';
          const block = {
            ...b,
            type,
            content,
            _id: Date.now() + Math.random()
          };

          if (type === 'chart') {
            const parsed = this.parseChartSpec(content);
            block._table = parsed.tableName || '';
            block._columns = parsed.columns || [];
            block._chartType = parsed.chartType || 'bar';
          }

          return block;
        });

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
        _table: '',
        _columns: [],
        _chartType: 'bar',
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
        if (block._table === table || block.content === table) {
          option.selected = true;
        }
        select.appendChild(option);
      });

      select.addEventListener('change', (e) => {
        block._table = e.target.value;
        block._columns = [];
        block.content = this.buildChartSpec(block._table || '', block._columns || [], block._chartType || 'bar');
        this.renderChartColumnSelector(block, wrapper);
      });

      const typeLabel = document.createElement('label');
      typeLabel.textContent = '默认类型:';

      const typeSelect = document.createElement('select');
      this.supportedChartTypes.forEach(t => {
        const option = document.createElement('option');
        option.value = t.type;
        option.textContent = t.name;
        if ((block._chartType || 'bar') === t.type) {
          option.selected = true;
        }
        typeSelect.appendChild(option);
      });
      typeSelect.addEventListener('change', (e) => {
        block._chartType = e.target.value;
        block.content = this.buildChartSpec(block._table || '', block._columns || [], block._chartType || 'bar');
        // 更新列选择器底部提示（如果存在）
        const footer = wrapper.querySelector('.chart-columns .muted');
        if (footer) {
          footer.textContent = `已选择 ${block._columns.length} 列：${block.content}`;
        }
      });

            const previewBtn = document.createElement('button');
            previewBtn.className = 'btn btn-secondary';
            previewBtn.textContent = '预览数据';
            previewBtn.addEventListener('click', () => this.previewTableData(block.content, wrapper));

      wrapper.appendChild(label);
      wrapper.appendChild(select);
      wrapper.appendChild(typeLabel);
      wrapper.appendChild(typeSelect);
      wrapper.appendChild(previewBtn);

      // 初次渲染列选择器
      this.renderChartColumnSelector(block, wrapper);

      return wrapper;
    }

    parseChartSpec(raw) {
      const text = (raw == null ? '' : String(raw)).trim();
      if (!text) return { tableName: '', columns: [] };
      // 支持 table(col1,col2)#type
      const [main, typePart] = text.split('#');
      const chartType = (typePart || '').trim().toLowerCase();

      const match = (main || '').trim().match(/^([A-Za-z0-9_]+)\s*(?:\(([^)]*)\))?$/);
      if (!match) return { tableName: text, columns: [] };
      const tableName = match[1];
      const colsPart = match[2];
      const columns = [];
      if (colsPart) {
        colsPart.split(',').forEach(p => {
          const c = (p || '').trim();
          if (c && !columns.includes(c)) columns.push(c);
        });
      }
      return { tableName, columns, chartType: chartType || null };
    }

    buildChartSpec(tableName, columns, chartType) {
      const t = (tableName == null ? '' : String(tableName)).trim();
      if (!t) return '';
      const cols = Array.isArray(columns) ? columns.filter(Boolean).map(c => String(c).trim()).filter(Boolean) : [];
      const base = cols.length === 0 ? t : `${t}(${cols.join(',')})`;
      const ct = (chartType == null ? '' : String(chartType)).trim().toLowerCase();
      if (!ct) return base;
      return `${base}#${ct}`;
    }

    async getTableColumns(tableName) {
      const t = (tableName == null ? '' : String(tableName)).trim();
      if (!t) return [];
      if (this.tableColumnsCache.has(t)) {
        return this.tableColumnsCache.get(t);
      }

      const req = new Request('获取表列名', 'api/database/columns');
      const resp = await req.get({ table: t });
      const cols = (resp.data && resp.data.data) ? resp.data.data : [];
      this.tableColumnsCache.set(t, cols);
      return cols;
    }

    renderChartColumnSelector(block, wrapper) {
      // 移除旧的列选择区
      const old = wrapper.querySelector('.chart-columns');
      if (old) old.remove();

      const tableName = block._table || '';
      if (!tableName) return;

      const container = document.createElement('div');
      container.className = 'chart-columns';
      container.style.cssText = 'width:100%; margin-top:10px; display:flex; flex-direction:column; gap:8px;';

      const hint = document.createElement('div');
      hint.className = 'muted';
      hint.textContent = '选择要展示的列（建议：第1列为维度，其余为数值列；至少选择2列）。';
      hint.style.cssText = 'color: var(--text-secondary); font-size: 13px;';
      container.appendChild(hint);

      const box = document.createElement('div');
      box.className = 'chart-columns-box';
      box.style.cssText = 'display:flex; flex-wrap:wrap; gap:8px;';
      container.appendChild(box);

      const footer = document.createElement('div');
      footer.className = 'muted';
      footer.style.cssText = 'color: var(--text-secondary); font-size: 12px;';
      footer.textContent = '加载列名中...';
      container.appendChild(footer);

      wrapper.appendChild(container);

      this.getTableColumns(tableName)
        .then(cols => {
          box.innerHTML = '';

          // 兼容旧数据：如果是纯表名（无选列），默认帮用户勾选前两列，避免“一张图塞进所有列”不直观
          if ((!block._columns || block._columns.length === 0) && (block.content === tableName || !block.content)) {
            block._columns = cols.slice(0, 2);
          }

          cols.forEach(col => {
            const id = `col-${block._id}-${col}`;
            const label = document.createElement('label');
            label.style.cssText = 'display:inline-flex; align-items:center; gap:6px; padding:4px 8px; border:1px solid var(--border-color); border-radius:999px; background: var(--bg-secondary);';

            const input = document.createElement('input');
            input.type = 'checkbox';
            input.id = id;
            input.checked = Array.isArray(block._columns) && block._columns.includes(col);
            input.addEventListener('change', () => {
              const selected = new Set(block._columns || []);
              if (input.checked) selected.add(col); else selected.delete(col);
              block._columns = Array.from(selected);
              block.content = this.buildChartSpec(block._table, block._columns, block._chartType || 'bar');
              footer.textContent = `已选择 ${block._columns.length} 列：${block.content}`;
            });

            const span = document.createElement('span');
            span.textContent = col;

            label.appendChild(input);
            label.appendChild(span);
            box.appendChild(label);
          });

          block.content = this.buildChartSpec(block._table, block._columns, block._chartType || 'bar');
          footer.textContent = `已选择 ${block._columns.length} 列：${block.content}`;
        })
        .catch(err => {
          console.error('加载列名失败', err);
          footer.textContent = '加载列名失败，请检查控制台';
        });
    }

        async previewTableData(tableName, containerEl) {
            if (!tableName) {
                alert('请先选择表名');
                return;
            }

      try {
        const req = new Request('预览表数据', 'api/database/preview');
        const parsed = this.parseChartSpec(tableName);
        const resp = await req.get({
          table: parsed.tableName,
          limit: 5,
          columns: parsed.columns && parsed.columns.length > 0 ? parsed.columns.join(',') : ''
        });
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
        .map(b => {
          if (b.type === 'chart') {
            const spec = this.buildChartSpec(b._table || '', b._columns || [], b._chartType || 'bar');
            return { type: 'chart', content: spec };
          }
          return { type: 'text', content: b.content || '' };
        })
        .filter(b => b.content && b.content.trim());

      if (blocks.length === 0) {
        alert('至少需要一个非空内容块');
        return;
      }

      // 基础校验：图表块至少选择 2 列（table(col1,col2)）或只填 table（表示全部列，可能很多列不直观）
      const invalidCharts = blocks.filter(b => b.type === 'chart')
        .map(b => this.parseChartSpec(b.content))
        .filter(p => p.tableName && p.columns && p.columns.length === 1);
      if (invalidCharts.length > 0) {
        alert('图表块选择列不完整：至少选择 2 列（维度列 + 数值列）。');
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

                // 检查是否是权限错误
                if (error.response && error.response.status === 403) {
                    this.showMessage('保存失败：你没有管理员权限，无法执行此操作', 'error');
                } else if (error.response && error.response.data && error.response.data.message) {
                    this.showMessage('保存失败：' + error.response.data.message, 'error');
                } else {
                    this.showMessage('保存失败', 'error');
                }
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

                // 检查是否是权限错误
                if (error.response && error.response.status === 403) {
                    this.showMessage('删除失败：你没有管理员权限，无法执行此操作', 'error');
                } else if (error.response && error.response.data && error.response.data.message) {
                    this.showMessage('删除失败：' + error.response.data.message, 'error');
                } else {
                    this.showMessage('删除失败', 'error');
                }
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
