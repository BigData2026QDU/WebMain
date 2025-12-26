import pandas as pd

# 读取原始CSV
df = pd.read_csv('test_cleaned.csv')

# 添加student_id列（使用行号）
df['student_id'] = range(1, len(df) + 1)

# 1. student_info表
student_info = df[['student_id', 'Age', 'Grade', 'Gender', 'Race', 'SES_Quartile', 'ParentalEducation']]
student_info.columns = ['student_id', 'age', 'grade', 'gender', 'race', 'ses_quartile', 'parental_education']
student_info.to_csv('student_info.csv', index=False, header=False)

# 2. student_scores表
student_scores = df[['student_id', 'TestScore_Math', 'TestScore_Reading', 'TestScore_Science', 'GPA']]
student_scores.columns = ['student_id', 'test_score_math', 'test_score_reading', 'test_score_science', 'gpa']
student_scores.to_csv('student_scores.csv', index=False, header=False)

# 3. student_behavior表
student_behavior = df[['student_id', 'AttendanceRate', 'StudyHours', 'Extracurricular', 'PartTimeJob']]
student_behavior.columns = ['student_id', 'attendance_rate', 'study_hours', 'extracurricular', 'part_time_job']
student_behavior.to_csv('student_behavior.csv', index=False, header=False)

# 4. school_info表
school_info = df[['student_id', 'SchoolType', 'Locale']]
school_info.columns = ['student_id', 'school_type', 'locale']
school_info.to_csv('school_info.csv', index=False, header=False)

# 5. student_lifestyle表
student_lifestyle = df[['student_id', 'InternetAccess', 'ParentSupport', 'Romantic', 'FreeTime', 'GoOut']]
student_lifestyle.columns = ['student_id', 'internet_access', 'parent_support', 'romantic', 'free_time', 'go_out']
student_lifestyle.to_csv('student_lifestyle.csv', index=False, header=False)

print("CSV文件拆分完成！")
print(f"总记录数: {len(df)}")
print("生成的文件:")
print("- student_info.csv")
print("- student_scores.csv")
print("- student_behavior.csv")
print("- school_info.csv")
print("- student_lifestyle.csv")
