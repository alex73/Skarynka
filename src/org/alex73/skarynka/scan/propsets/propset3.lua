--[[
GENERATED PROPCASE TABLE
use propcase.lua instead
--]]
return {
  DRIVE_MODE=102,
  FOCUS_MODE=133,     --WIKI|Propcase manual_focus
  REAL_FOCUS_MODE=6,       --WIKI|Propcase focus_mode
  FOCUS_STATE=18,
  FLASH_MODE=143,
  FLASH_FIRE=122,
  FLASH_MANUAL_OUTPUT=141,
  FLASH_ADJUST_MODE=121,
  USER_TV=266,
  TV=264,
  USER_AV=26,
  AV=23,
  MIN_AV=25,
  SV=249,
  DELTA_SV=79,
  SV_MARKET=248,
  BV=34,
  SUBJECT_DIST1=247,
  SUBJECT_DIST2=65,
  DATE_STAMP=66,         -- 0 = Off, 1 = Date, 2 = Date & Time
  ISO_MODE=149,
  EXPOSURE_LOCK=208,     -- Old PROPCASE_SHOOTING value - gets set when set_aelock called or AEL button pressed
  SHOOTING=301,     -- This value appears to work better - gets set to 1 when camera has focused and set exposure, returns to 0 after shot
  IS_FLASH_READY=210, 
  OVEREXPOSURE=103,
  SHOOTING_MODE=49,
  IS_MODE=145,
  QUALITY=57,
  RESOLUTION=220,
  EV_CORRECTION_1=107,
  EV_CORRECTION_2=209,
  ORIENTATION_SENSOR=221,
  DIGITAL_ZOOM_MODE=91,     -- Digital Zoom Mode/State 0 = off/standard, 2 = 1.4x, 3 = 2.3x
  DIGITAL_ZOOM_STATE=94,
  DIGITAL_ZOOM_POSITION=95,
  DISPLAY_MODE=105,
  BRACKET_MODE=29,
  FLASH_SYNC_CURTAIN=64,
  METERING_MODE=155,
  WB_MODE=270,     -- 0 = Auto, 1 = Daylight, 2 = Cloudy, 3 = Tungsten, 4 = Fluorescent, 5 = Fluorescent H, 7 = Custom
  WB_ADJ=271,
  ASPECT_RATIO=294,
  TIMER_MODE=225,
  OPTICAL_ZOOM_POSITION=197,
  VIDEO_RESOLUTION=169,
  AF_ASSIST_BEAM=5,       -- 0=disabled,  1=enabled
  AF_LOCK=11,      -- 0 = AF not locked, 1 = AF locked
  CONTINUOUS_AF=12,      -- 0 = Continuous AF off, 1 = Continuous AF on
  SERVO_AF=297,     -- 0 = Servo AF off, 1 = Servo AF on
  TIMER_SHOTS=336,     -- Number of shots for TIMER_MODE=Custom
}
