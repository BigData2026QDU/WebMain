/**
 * ChartFactory - 图表配置生成器
 * 根据图表类型和解析后的数据生成 ECharts 配置
 *
 * 特性：
 * - 智能双Y轴：自动检测数据量级差异，当差异超过100倍时自动启用双轴
 * - 支持图表类型：bar, line, pie, scatter, mix
 */
(function() {
  'use strict';

  // 量级差异阈值：超过此倍数自动启用双轴
  const MAGNITUDE_THRESHOLD = 100;

  class ChartFactory {
    /**
     * 创建图表配置
     * @param {string} chartType - 图表类型 (bar, line, pie, scatter, mix)
     * @param {Object} parsedData - 经 ChartParser 解析后的数据
     * @param {Object} themeColors - 主题颜色
     * @param {Object} options - 额外配置选项
     * @returns {Object} ECharts 配置对象
     */
    static create(chartType, parsedData, themeColors = {}, options = {}) {
      const colors = {
        bgPrimary: '#ffffff',
        bgSecondary: '#f5f5f5',
        textPrimary: '#1a1a1a',
        textSecondary: '#4a4a4a',
        border: '#d0d0d0',
        accentPrimary: '#3b82f6',
        accentSecondary: '#60a5fa',
        seriesColors: ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899'],
        ...themeColors
      };

      const baseConfig = {
        backgroundColor: colors.bgPrimary,
        textStyle: {
          color: colors.textPrimary,
          fontFamily: 'inherit'
        },
        tooltip: {
          trigger: 'axis',
          textStyle: { color: colors.textPrimary },
          backgroundColor: colors.bgSecondary,
          borderColor: colors.border
        },
        grid: {
          left: 12,
          right: 12,
          top: 48,
          bottom: 24,
          containLabel: true
        }
      };

      switch (chartType) {
        case 'bar':
          return this._buildBar(parsedData, colors, baseConfig, options);
        case 'line':
          return this._buildLine(parsedData, colors, baseConfig, options);
        case 'pie':
          return this._buildPie(parsedData, colors, baseConfig, options);
        case 'scatter':
          return this._buildScatter(parsedData, colors, baseConfig, options);
        case 'mix':
          return this._buildMix(parsedData, colors, baseConfig, options);
        default:
          console.warn(`ChartFactory: 未知图表类型 "${chartType}"，使用默认柱状图`);
          return this._buildBar(parsedData, colors, baseConfig, options);
      }
    }

    /**
     * 智能分析数据量级，决定是否需要双Y轴
     * @private
     */
    static _analyzeMagnitude(series) {
      if (!series || series.length < 2) {
        return { needDualAxis: false };
      }

      // 计算每个系列的最大绝对值
      const seriesStats = series.map((s, idx) => {
        const validData = s.data.filter(v => v != null && !isNaN(v) && v !== 0);
        const max = validData.length > 0 ? Math.max(...validData.map(Math.abs)) : 0;
        return { idx, name: s.name, max };
      }).filter(s => s.max > 0);

      if (seriesStats.length < 2) {
        return { needDualAxis: false };
      }

      // 按最大值降序排序
      seriesStats.sort((a, b) => b.max - a.max);

      const largest = seriesStats[0].max;
      const smallest = seriesStats[seriesStats.length - 1].max;
      const ratio = largest / smallest;

      // 如果量级差异超过阈值，启用双轴
      if (ratio > MAGNITUDE_THRESHOLD) {
        // 找到最大的量级断层位置
        let maxGapIdx = 0;
        let maxGap = 1;
        for (let i = 0; i < seriesStats.length - 1; i++) {
          const gap = seriesStats[i].max / seriesStats[i + 1].max;
          if (gap > maxGap) {
            maxGap = gap;
            maxGapIdx = i;
          }
        }

        // 断层位置之前的是大数值（右轴），之后的是小数值（左轴）
        const rightAxisIndices = seriesStats.slice(0, maxGapIdx + 1).map(s => s.idx);
        const leftAxisIndices = seriesStats.slice(maxGapIdx + 1).map(s => s.idx);

        if (leftAxisIndices.length > 0 && rightAxisIndices.length > 0) {
          return {
            needDualAxis: true,
            leftAxisIndices,
            rightAxisIndices,
            ratio
          };
        }
      }

      return { needDualAxis: false, ratio };
    }

    /**
     * 构建双Y轴配置
     * @private
     */
    static _buildDualYAxis(colors, leftName = '', rightName = '') {
      return [
        {
          type: 'value',
          name: leftName,
          position: 'left',
          axisLabel: {
            color: colors.textSecondary,
            formatter: this._formatAxisLabel
          },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        },
        {
          type: 'value',
          name: rightName,
          position: 'right',
          axisLabel: {
            color: colors.textSecondary,
            formatter: this._formatAxisLabel
          },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { show: false }
        }
      ];
    }

    /**
     * 格式化Y轴标签（大数字简化显示）
     * @private
     */
    static _formatAxisLabel(value) {
      if (value === 0) return '0';
      const abs = Math.abs(value);
      if (abs >= 1e8) return (value / 1e8).toFixed(1) + '亿';
      if (abs >= 1e4) return (value / 1e4).toFixed(1) + '万';
      if (abs >= 1e3) return (value / 1e3).toFixed(1) + 'K';
      return value;
    }

    /**
     * 构建柱状图配置
     * @private
     */
    static _buildBar(parsedData, colors, baseConfig, options = {}) {
      const {
        horizontal = false,
        stack = false,
        showLabel = false
      } = options;

      const { dimensions, series } = parsedData;
      const magnitude = this._analyzeMagnitude(series);

      const config = {
        ...baseConfig,
        color: colors.seriesColors,
        legend: series.length > 1 ? {
          top: 10,
          textStyle: { color: colors.textPrimary }
        } : undefined,
        tooltip: {
          ...baseConfig.tooltip,
          axisPointer: { type: 'shadow' },
          formatter: this._buildTooltipFormatter(series)
        }
      };

      // 根据是否需要双轴构建配置
      if (magnitude.needDualAxis && !horizontal) {
        config.grid.right = 60; // 为右轴留出空间

        config.yAxis = this._buildDualYAxis(colors);
        config.xAxis = {
          type: 'category',
          data: dimensions,
          axisLabel: {
            color: colors.textSecondary,
            rotate: dimensions.length > 6 ? 45 : 0
          },
          axisLine: { lineStyle: { color: colors.border } },
          axisTick: { show: false }
        };

        config.series = series.map((s, idx) => ({
          name: s.name,
          type: 'bar',
          data: s.data,
          yAxisIndex: magnitude.rightAxisIndices.includes(idx) ? 1 : 0,
          itemStyle: { borderRadius: [4, 4, 0, 0] },
          label: showLabel ? {
            show: true,
            position: 'top',
            color: colors.textPrimary,
            formatter: (p) => this._formatValue(p.value)
          } : undefined,
          emphasis: {
            itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' }
          }
        }));

      } else {
        // 单Y轴（或横向柱状图）
        if (horizontal) {
          config.xAxis = {
            type: 'value',
            axisLabel: { color: colors.textSecondary, formatter: this._formatAxisLabel },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
          };
          config.yAxis = {
            type: 'category',
            data: dimensions,
            axisLabel: { color: colors.textPrimary },
            axisLine: { lineStyle: { color: colors.border } },
            axisTick: { show: false }
          };
        } else {
          config.xAxis = {
            type: 'category',
            data: dimensions,
            axisLabel: {
              color: colors.textSecondary,
              rotate: dimensions.length > 6 ? 45 : 0
            },
            axisLine: { lineStyle: { color: colors.border } },
            axisTick: { show: false }
          };
          config.yAxis = {
            type: 'value',
            axisLabel: { color: colors.textSecondary, formatter: this._formatAxisLabel },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
          };
        }

        config.series = series.map((s) => ({
          name: s.name,
          type: 'bar',
          data: s.data,
          barWidth: series.length === 1 ? '50%' : undefined,
          stack: stack ? 'total' : undefined,
          itemStyle: { borderRadius: horizontal ? [0, 4, 4, 0] : [4, 4, 0, 0] },
          label: showLabel ? {
            show: true,
            position: horizontal ? 'right' : 'top',
            color: colors.textPrimary,
            formatter: (p) => this._formatValue(p.value)
          } : undefined,
          emphasis: {
            itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0,0,0,0.2)' }
          }
        }));
      }

      return config;
    }

    /**
     * 构建折线图配置
     * @private
     */
    static _buildLine(parsedData, colors, baseConfig, options = {}) {
      const {
        smooth = true,
        area = false,
        showPoints = true
      } = options;

      const { dimensions, series } = parsedData;
      const magnitude = this._analyzeMagnitude(series);

      const config = {
        ...baseConfig,
        color: colors.seriesColors,
        legend: series.length > 1 ? {
          top: 10,
          textStyle: { color: colors.textPrimary }
        } : undefined,
        tooltip: {
          ...baseConfig.tooltip,
          trigger: 'axis',
          formatter: this._buildTooltipFormatter(series)
        },
        xAxis: {
          type: 'category',
          data: dimensions,
          boundaryGap: false,
          axisLabel: {
            color: colors.textSecondary,
            rotate: dimensions.length > 8 ? 30 : 0
          },
          axisLine: { lineStyle: { color: colors.border } }
        }
      };

      if (magnitude.needDualAxis) {
        config.grid.right = 60;
        config.yAxis = this._buildDualYAxis(colors);

        config.series = series.map((s, idx) => ({
          name: s.name,
          type: 'line',
          data: s.data,
          yAxisIndex: magnitude.rightAxisIndices.includes(idx) ? 1 : 0,
          smooth,
          showSymbol: showPoints,
          symbolSize: 6,
          areaStyle: area ? { opacity: 0.3 } : undefined,
          lineStyle: { width: 2 },
          emphasis: { focus: 'series', lineStyle: { width: 3 } }
        }));
      } else {
        config.yAxis = {
          type: 'value',
          axisLabel: { color: colors.textSecondary, formatter: this._formatAxisLabel },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        };

        config.series = series.map(s => ({
          name: s.name,
          type: 'line',
          data: s.data,
          smooth,
          showSymbol: showPoints,
          symbolSize: 6,
          areaStyle: area ? { opacity: 0.3 } : undefined,
          lineStyle: { width: 2 },
          emphasis: { focus: 'series', lineStyle: { width: 3 } }
        }));
      }

      return config;
    }

    /**
     * 构建饼图配置
     * @private
     */
    static _buildPie(parsedData, colors, baseConfig, options = {}) {
      const {
        radius = ['40%', '70%'],
        showLabel = true,
        showPercentage = true,
        roseType = false
      } = options;

      let pieData;
      if (Array.isArray(parsedData)) {
        pieData = parsedData;
      } else if (parsedData.dimensions && parsedData.series) {
        pieData = parsedData.dimensions.map((name, idx) => ({
          name,
          value: parsedData.series[0]?.data[idx] || 0
        }));
      } else {
        console.error('无法解析饼图数据格式');
        return baseConfig;
      }

      return {
        ...baseConfig,
        color: colors.seriesColors,
        tooltip: {
          trigger: 'item',
          textStyle: { color: colors.textPrimary },
          backgroundColor: colors.bgSecondary,
          borderColor: colors.border,
          formatter: showPercentage ? '{a} <br/>{b}: {c} ({d}%)' : '{a} <br/>{b}: {c}'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          top: 'center',
          textStyle: { color: colors.textPrimary }
        },
        series: [{
          name: '数据',
          type: 'pie',
          radius,
          roseType: roseType ? 'radius' : undefined,
          data: pieData,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          label: showLabel ? {
            color: colors.textPrimary,
            formatter: showPercentage ? '{b}: {d}%' : '{b}: {c}'
          } : { show: false },
          labelLine: showLabel ? undefined : { show: false }
        }]
      };
    }

    /**
     * 构建散点图配置
     * @private
     */
    static _buildScatter(parsedData, colors, baseConfig, options = {}) {
      const { symbolSize = 10 } = options;
      const { data, xAxisName, yAxisName, seriesName } = parsedData;

      return {
        ...baseConfig,
        color: colors.seriesColors,
        tooltip: {
          trigger: 'item',
          textStyle: { color: colors.textPrimary },
          backgroundColor: colors.bgSecondary,
          borderColor: colors.border,
          formatter: (params) => {
            const point = params.data;
            let text = `${params.seriesName}<br/>${xAxisName}: ${point[0]}<br/>${yAxisName}: ${point[1]}`;
            if (point[2] !== undefined) text += `<br/>大小: ${point[2]}`;
            return text;
          }
        },
        xAxis: {
          type: 'value',
          name: xAxisName,
          nameTextStyle: { color: colors.textPrimary },
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        },
        yAxis: {
          type: 'value',
          name: yAxisName,
          nameTextStyle: { color: colors.textPrimary },
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        },
        series: [{
          name: seriesName,
          type: 'scatter',
          data,
          symbolSize,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)',
              borderColor: colors.accentPrimary,
              borderWidth: 2
            }
          }
        }]
      };
    }

    /**
     * 构建混合图表（柱状图+折线图）
     * @private
     */
    static _buildMix(parsedData, colors, baseConfig, options = {}) {
      const {
        barSeriesIndex = [0],
        lineSeriesIndex = []
      } = options;

      const { dimensions, series } = parsedData;
      const magnitude = this._analyzeMagnitude(series);

      const config = {
        ...baseConfig,
        color: colors.seriesColors,
        legend: {
          top: 10,
          textStyle: { color: colors.textPrimary }
        },
        tooltip: {
          ...baseConfig.tooltip,
          trigger: 'axis',
          axisPointer: { type: 'cross' },
          formatter: this._buildTooltipFormatter(series)
        },
        xAxis: {
          type: 'category',
          data: dimensions,
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } }
        }
      };

      if (magnitude.needDualAxis) {
        config.grid.right = 60;
        config.yAxis = this._buildDualYAxis(colors);

        config.series = series.map((s, idx) => {
          const isBar = barSeriesIndex.includes(idx) || (!lineSeriesIndex.includes(idx) && idx === 0);
          return {
            name: s.name,
            type: isBar ? 'bar' : 'line',
            data: s.data,
            yAxisIndex: magnitude.rightAxisIndices.includes(idx) ? 1 : 0,
            ...(isBar ? {
              itemStyle: { borderRadius: [4, 4, 0, 0] }
            } : {
              smooth: true,
              lineStyle: { width: 2 }
            })
          };
        });
      } else {
        config.yAxis = {
          type: 'value',
          axisLabel: { color: colors.textSecondary, formatter: this._formatAxisLabel },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border, type: 'dashed' } }
        };

        config.series = series.map((s, idx) => {
          const isBar = barSeriesIndex.includes(idx) || (!lineSeriesIndex.includes(idx) && idx === 0);
          return {
            name: s.name,
            type: isBar ? 'bar' : 'line',
            data: s.data,
            ...(isBar ? {
              barWidth: '50%',
              itemStyle: { borderRadius: [4, 4, 0, 0] }
            } : {
              smooth: true,
              lineStyle: { width: 2 }
            })
          };
        });
      }

      return config;
    }

    /**
     * 格式化数值显示
     * @private
     */
    static _formatValue(value) {
      if (value == null || isNaN(value)) return '-';
      const abs = Math.abs(value);
      if (abs >= 1e8) return (value / 1e8).toFixed(2) + '亿';
      if (abs >= 1e4) return (value / 1e4).toFixed(2) + '万';
      if (abs >= 1e3) return value.toLocaleString();
      if (Number.isInteger(value)) return value;
      return value.toFixed(2);
    }

    /**
     * 构建 Tooltip formatter
     * @private
     */
    static _buildTooltipFormatter(series) {
      const formatValue = this._formatValue;
      return function(params) {
        if (!Array.isArray(params)) params = [params];
        let result = params[0]?.axisValue || '';
        params.forEach(p => {
          const marker = p.marker || '';
          const value = formatValue(p.value);
          result += `<br/>${marker}${p.seriesName}: ${value}`;
        });
        return result;
      };
    }

    /**
     * 获取支持的图表类型列表
     */
    static getSupportedTypes() {
      return [
        { type: 'bar', name: '柱状图', description: '适合对比分类数据' },
        { type: 'line', name: '折线图', description: '适合展示趋势变化' },
        { type: 'pie', name: '饼图', description: '适合展示占比关系' },
        { type: 'scatter', name: '散点图', description: '适合展示相关性' },
        { type: 'mix', name: '混合图', description: '柱状图和折线图组合' }
      ];
    }
  }

  // 导出到全局
  if (typeof window !== 'undefined') {
    window.ChartFactory = ChartFactory;
  }

  if (typeof module !== 'undefined' && module.exports) {
    module.exports = ChartFactory;
  }
})();
