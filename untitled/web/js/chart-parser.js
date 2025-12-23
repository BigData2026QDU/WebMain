/**
 * ChartParser - 多列多行数据解析器
 * 负责将数据库格式的数据转换为图表可用的格式
 */
(function() {
  'use strict';

  class ChartParser {
    /**
     * 解析多列多行数据
     * @param {Object} chartData - 包含 columns 和 rows 的数据对象
     * @param {Object} config - 解析配置
     * @returns {Object} 解析后的数据结构
     */
    static parse(chartData, config = {}) {
      if (!chartData || !chartData.columns || !chartData.rows) {
        throw new Error('ChartParser: 数据格式错误，需要包含 columns 和 rows');
      }

      const {
        xAxisColumn = 0,  // X轴使用第几列（索引）
        seriesColumns = null  // 系列列索引数组，null表示除X轴外的所有列
      } = config;

      const { columns, rows } = chartData;

      // 验证数据完整性
      if (columns.length === 0 || rows.length === 0) {
        throw new Error('ChartParser: columns 或 rows 不能为空');
      }

      // 确定系列列
      const actualSeriesColumns = seriesColumns ||
        columns.map((_, idx) => idx).filter(idx => idx !== xAxisColumn);

      if (actualSeriesColumns.length === 0) {
        throw new Error('ChartParser: 至少需要一个数据系列列');
      }

      // 提取维度（X轴）数据
      const dimensions = rows.map(row => {
        const value = row[xAxisColumn];
        return value != null ? String(value) : '';
      });

      // 提取系列数据
      const series = actualSeriesColumns.map(colIdx => ({
        name: columns[colIdx] || `系列${colIdx}`,
        data: rows.map(row => {
          const value = row[colIdx];
          return value != null ? Number(value) : 0;
        })
      }));

      return {
        dimensions,           // X轴维度数据
        series,               // 多个系列的数据
        dimensionName: columns[xAxisColumn] || '维度',
        seriesNames: series.map(s => s.name),
        rawColumns: columns,
        rawRows: rows,
        seriesColumns: actualSeriesColumns  // 添加：系列对应的原始列索引
      };
    }

    /**
     * 解析饼图数据（特殊处理）
     * 饼图只需要名称和数值两列
     * @param {Object} chartData
     * @param {Object} config
     * @returns {Array} 饼图数据
     */
    static parsePie(chartData, config = {}) {
      const {
        nameColumn = 0,
        valueColumn = 1
      } = config;

      const { columns, rows } = chartData;

      if (!columns || !rows) {
        throw new Error('ChartParser: 饼图数据格式错误');
      }

      return rows.map(row => ({
        name: row[nameColumn] != null ? String(row[nameColumn]) : '未知',
        value: row[valueColumn] != null ? Number(row[valueColumn]) : 0
      }));
    }

    /**
     * 解析散点图数据
     * @param {Object} chartData
     * @param {Object} config
     * @returns {Object}
     */
    static parseScatter(chartData, config = {}) {
      const {
        xColumn = 0,
        yColumn = 1,
        nameColumn = null,
        sizeColumn = null
      } = config;

      const { columns, rows } = chartData;

      const data = rows.map(row => {
        const point = [
          row[xColumn] != null ? Number(row[xColumn]) : 0,
          row[yColumn] != null ? Number(row[yColumn]) : 0
        ];

        // 支持带大小的散点
        if (sizeColumn != null && row[sizeColumn] != null) {
          point.push(Number(row[sizeColumn]));
        }

        return point;
      });

      return {
        data,
        xAxisName: columns[xColumn] || 'X',
        yAxisName: columns[yColumn] || 'Y',
        seriesName: nameColumn != null ? columns[nameColumn] : '数据点'
      };
    }

    /**
     * 智能检测最佳图表类型
     * @param {Object} chartData
     * @returns {string}
     */
    static suggestChartType(chartData) {
      const { columns, rows } = chartData;

      if (!columns || !rows || rows.length === 0) {
        return 'bar';
      }

      const numColumns = columns.length;
      const numRows = rows.length;

      // 2列数据
      if (numColumns === 2) {
        // 数据行少，适合饼图
        if (numRows <= 8) {
          return 'pie';
        }
        // 数据行多，适合柱状图
        return 'bar';
      }

      // 3列以上，检查是否适合多系列
      if (numColumns >= 3) {
        // 第一列是文本，其他是数字 -> 多系列柱状图/折线图
        const firstColIsText = rows.every(row => isNaN(Number(row[0])));
        const restAreNumbers = rows.every(row =>
          row.slice(1).every(val => !isNaN(Number(val)))
        );

        if (firstColIsText && restAreNumbers) {
          return numColumns > 4 ? 'line' : 'bar';
        }
      }

      return 'bar';
    }

    /**
     * 验证数据格式
     * @param {Object} chartData
     * @returns {Object} { valid: boolean, errors: string[] }
     */
    static validate(chartData) {
      const errors = [];

      if (!chartData) {
        errors.push('数据对象不能为空');
        return { valid: false, errors };
      }

      if (!Array.isArray(chartData.columns)) {
        errors.push('columns 必须是数组');
      }

      if (!Array.isArray(chartData.rows)) {
        errors.push('rows 必须是数组');
      }

      if (chartData.columns && chartData.rows) {
        const colCount = chartData.columns.length;
        chartData.rows.forEach((row, idx) => {
          if (!Array.isArray(row)) {
            errors.push(`第 ${idx} 行不是数组`);
          } else if (row.length !== colCount) {
            errors.push(`第 ${idx} 行列数(${row.length})与表头列数(${colCount})不匹配`);
          }
        });
      }

      return {
        valid: errors.length === 0,
        errors
      };
    }
  }

  // 导出到全局
  if (typeof window !== 'undefined') {
    window.ChartParser = ChartParser;
  }

  // CommonJS/Node.js 支持
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = ChartParser;
  }
})();
