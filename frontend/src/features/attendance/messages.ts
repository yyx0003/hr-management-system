export const ATTENDANCE_MESSAGES = {
  loadFailed: '勤怠情報の取得に失敗しました。',
  registerSuccess: '勤怠情報を登録しました。',
  registerFailed: '勤怠情報の登録に失敗しました。',
  updateSuccess: '勤怠情報を更新しました。',
  updateFailed: '勤怠情報の更新に失敗しました。',
  csvFileRequired: 'CSVファイルを選択してください。',
  csvFileTypeInvalid: 'CSVファイルを選択してください。',
  csvImportSuccess: 'CSVファイルの取込が完了しました。',
  csvImportFailed: 'CSVファイルの取込に失敗しました。',
  csvExportFailed: 'CSVファイルの出力に失敗しました。',
  targetMonthRequired: '対象年月を選択してください。',
  timeRequired: '出勤時刻と退勤時刻を入力してください。',
  timeNotAllowed:
    '有給または欠勤の場合、出勤時刻と退勤時刻は入力できません。',
  startAfterEnd: '退勤時刻は出勤時刻より後にしてください。',
  unexpectedError: '予期せぬエラーが発生しました。',
} as const