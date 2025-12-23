/**
 * ChartFactory - 图表配置生成器
 * 根据图表类型和解析后的数据生成 ECharts 配置
 */
(function() {
  'use strict';

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
      // 默认主题颜色
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

      // 根据图表类型调用相应的构建器
      switch (chartType) {
        case 'bar':
          return this.buildBar(parsedData, colors, baseConfig, options);
        case 'line':
          return this.buildLine(parsedData, colors, baseConfig, options);
        case 'pie':
          return this.buildPie(parsedData, colors, baseConfig, options);
        case 'scatter':
          return this.buildScatter(parsedData, colors, baseConfig, options);
        case 'mix':
          return this.buildMix(parsedData, colors, baseConfig, options);
        default:
          console.warn(`ChartFactory: 未知图表类型 "${chartType}"，使用默认柱状图`);
          return this.buildBar(parsedData, colors, baseConfig, options);
      }
    }

    /**
     * 构建柱状图配置
     */
    static buildBar(parsedData, colors, baseConfig, options = {}) {
      const {
        horizontal = false,  // 是否横向
        stack = false,       // 是否堆叠
        showLabel = false,   // 是否显示数值标签
        dualAxis = false,    // 是否使用双Y轴
        leftAxisColumns = [], // 左Y轴列索引
        rightAxisColumns = [] // 右Y轴列索引
      } = options;

      const { dimensions, series, dimensionName } = parsedData;

      const config = {
        ...baseConfig,
        color: colors.seriesColors,
        legend: series.length > 1 ? {
          top: 10,
          textStyle: { color: colors.textPrimary }
        } : undefined,
        tooltip: {
          ...baseConfig.tooltip,
          axisPointer: { type: 'shadow' }
        }
      };

      // 双Y轴配置
      if (dualAxis && leftAxisColumns.length > 0 && rightAxisColumns.length > 0) {
        config.yAxis = [
          {
            type: 'value',
            name: '小数值',
            position: 'left',
            axisLabel: { color: colors.textSecondary },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { lineStyle: { color: colors.border } }
          },
          {
            type: 'value',
            name: '大数值',
            position: 'right',
            axisLabel: { color: colors.textSecondary },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { show: false }
          }
        ];

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

        // 构建系列，指定使用哪个Y轴
        config.series = series.map((s, idx) => {
          // 注意：series的索引idx对应rawColumns中除X轴外的列
          // 如果seriesColumns存在，需要映射回原始列索引
          const originalColumnIndex = parsedData.rawColumns ?
            (parsedData.seriesColumns ? parsedData.seriesColumns[idx] : idx + 1) : idx + 1;

          const useRightAxis = rightAxisColumns.includes(originalColumnIndex);

          return {
            name: s.name,
            type: 'bar',
            data: s.data,
            yAxisIndex: useRightAxis ? 1 : 0, // 0=左轴, 1=右轴
            itemStyle: {
              borderRadius: [6, 6, 0, 0]
            },
            label: showLabel ? {
              show: true,
              position: 'top',
              color: colors.textPrimary,
              formatter: function(params) {
                // 格式化大数字显示
                const val = params.value;
                if (val >= 1000000) return (val / 1000000).toFixed(1) + 'M';
                if (val >= 1000) return (val / 1000).toFixed(1) + 'K';
                return val;
              }
            } : undefined,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowColor: 'rgba(0, 0, 0, 0.3)'
              }
            }
          };
        });

      } else {
        // 原有单Y轴逻辑
        if (horizontal) {
          config.xAxis = {
            type: 'value',
            axisLabel: { color: colors.textSecondary },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { lineStyle: { color: colors.border } }
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
            axisLabel: { color: colors.textSecondary },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { lineStyle: { color: colors.border } }
          };
        }

        config.series = series.map((s, idx) => ({
          name: s.name,
          type: 'bar',
          data: s.data,
          barWidth: series.length === 1 ? '50%' : undefined,
          stack: stack ? 'total' : undefined,
          itemStyle: {
            borderRadius: horizontal ? [0, 6, 6, 0] : [6, 6, 0, 0]
          },
          label: showLabel ? {
            show: true,
            position: horizontal ? 'right' : 'top',
            color: colors.textPrimary
          } : undefined,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.3)'
            }
          }
        }));
      }

      return config;
    }

    /**
     * 构建折线图配置
     */
    static buildLine(parsedData, colors, baseConfig, options = {}) {
      const {
        smooth = true,       // 是否平滑曲线
        area = false,        // 是否显示面积
        showPoints = true,   // 是否显示数据点
        dualAxis = false,    // 是否使用双Y轴
        leftAxisColumns = [],
        rightAxisColumns = []
      } = options;

      const { dimensions, series } = parsedData;

      const config = {
        ...baseConfig,
        color: colors.seriesColors,
        legend: series.length > 1 ? {
          top: 10,
          textStyle: { color: colors.textPrimary }
        } : undefined,
        tooltip: {
          ...baseConfig.tooltip,
          trigger: 'axis'
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

      // 双Y轴配置
      if (dualAxis && leftAxisColumns.length > 0 && rightAxisColumns.length > 0) {
        config.yAxis = [
          {
            type: 'value',
            name: '小数值',
            position: 'left',
            axisLabel: { color: colors.textSecondary },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { lineStyle: { color: colors.border } }
          },
          {
            type: 'value',
            name: '大数值',
            position: 'right',
            axisLabel: { color: colors.textSecondary },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { show: false }
          }
        ];

        config.series = series.map((s, idx) => {
          const originalColumnIndex = parsedData.seriesColumns ? parsedData.seriesColumns[idx] : idx + 1;
          const useRightAxis = rightAxisColumns.includes(originalColumnIndex);

          return {
            name: s.name,
            type: 'line',
            data: s.data,
            yAxisIndex: useRightAxis ? 1 : 0,
            smooth: smooth,
            showSymbol: showPoints,
            symbolSize: 6,
            areaStyle: area ? { opacity: 0.3 } : undefined,
            lineStyle: { width: 2 },
            emphasis: {
              focus: 'series',
              lineStyle: { width: 3 }
            }
          };
        });
      } else {
        // 单Y轴
        config.yAxis = {
          type: 'value',
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border } }
        };

        config.series = series.map(s => ({
          name: s.name,
          type: 'line',
          data: s.data,
          smooth: smooth,
          showSymbol: showPoints,
          symbolSize: 6,
          areaStyle: area ? { opacity: 0.3 } : undefined,
          lineStyle: { width: 2 },
          emphasis: {
            focus: 'series',
            lineStyle: { width: 3 }
          }
        }));
      }

      return config;
    }

    /**
     * 构建饼图配置
     */
    static buildPie(parsedData, colors, baseConfig, options = {}) {
      const {
        radius = ['40%', '70%'],  // 内外半径（环形图）
        showLabel = true,
        showPercentage = true,
        roseType = false  // 是否玫瑰图
      } = options;

      // 饼图数据格式特殊处理
      let pieData;

      if (Array.isArray(parsedData)) {
        // 已经是饼图格式
        pieData = parsedData;
      } else if (parsedData.dimensions && parsedData.series) {
        // 从多系列数据转换：使用第一个系列
        pieData = parsedData.dimensions.map((name, idx) => ({
          name,
          value: parsedData.series[0]?.data[idx] || 0
        }));
      } else {
        console.error('无法解析饼图数据格式');
        return baseConfig;
      }

      const config = {
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
        series: [
          {
            name: '数据',
            type: 'pie',
            radius: radius,
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
          }
        ]
      };

      return config;
    }

    /**
     * 构建散点图配置
     */
    static buildScatter(parsedData, colors, baseConfig, options = {}) {
      const {
        symbolSize = 10,
        showRegressionLine = false  // 是否显示回归线
      } = options;

      const { data, xAxisName, yAxisName, seriesName } = parsedData;

      const config = {
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
            if (point[2] !== undefined) {
              text += `<br/>大小: ${point[2]}`;
            }
            return text;
          }
        },
        xAxis: {
          type: 'value',
          name: xAxisName,
          nameTextStyle: { color: colors.textPrimary },
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border } }
        },
        yAxis: {
          type: 'value',
          name: yAxisName,
          nameTextStyle: { color: colors.textPrimary },
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border } }
        },
        series: [
          {
            name: seriesName,
            type: 'scatter',
            data: data,
            symbolSize: symbolSize,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowColor: 'rgba(0, 0, 0, 0.5)',
                borderColor: colors.accentPrimary,
                borderWidth: 2
              }
            }
          }
        ]
      };

      return config;
    }

    /**
     * 构建混合图表（柱状图+折线图）
     */
    static buildMix(parsedData, colors, baseConfig, options = {}) {
      const {
        barSeriesIndex = [0],    // 哪些系列显示为柱状图
        lineSeriesIndex = [1],   // 哪些系列显示为折线图
        dualAxis = false,
        leftAxisColumns = [],
        rightAxisColumns = []
      } = options;

      const { dimensions, series } = parsedData;

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
          axisPointer: { type: 'cross' }
        },
        xAxis: {
          type: 'category',
          data: dimensions,
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } }
        }
      };

      // 双Y轴配置
      if (dualAxis && leftAxisColumns.length > 0 && rightAxisColumns.length > 0) {
        config.yAxis = [
          {
            type: 'value',
            name: '主轴',
            position: 'left',
            axisLabel: { color: colors.textSecondary },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { lineStyle: { color: colors.border } }
          },
          {
            type: 'value',
            name: '副轴',
            position: 'right',
            axisLabel: { color: colors.textSecondary },
            axisLine: { lineStyle: { color: colors.border } },
            splitLine: { show: false }
          }
        ];

        config.series = series.map((s, idx) => {
          const originalColumnIndex = parsedData.seriesColumns ? parsedData.seriesColumns[idx] : idx + 1;
          const useRightAxis = rightAxisColumns.includes(originalColumnIndex);
          const isBar = barSeriesIndex.includes(idx);

          return {
            name: s.name,
            type: isBar ? 'bar' : 'line',
            data: s.data,
            yAxisIndex: useRightAxis ? 1 : 0,
            ...(isBar ? {
              itemStyle: { borderRadius: [6, 6, 0, 0] }
            } : {
              smooth: true,
              lineStyle: { width: 2 }
            })
          };
        });
      } else {
        // 单Y轴
        config.yAxis = {
          type: 'value',
          axisLabel: { color: colors.textSecondary },
          axisLine: { lineStyle: { color: colors.border } },
          splitLine: { lineStyle: { color: colors.border } }
        };

        config.series = series.map((s, idx) => {
          const isBar = barSeriesIndex.includes(idx);
          return {
            name: s.name,
            type: isBar ? 'bar' : 'line',
            data: s.data,
            ...(isBar ? {
              barWidth: '50%',
              itemStyle: { borderRadius: [6, 6, 0, 0] }
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

  // CommonJS/Node.js 支持
  if (typeof module !== 'undefined' && module.exports) {
    module.exports = ChartFactory;
  }
})();
