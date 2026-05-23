# MaintenanceIT

MaintenanceIT is an Android application for IT asset inventory, repair management, and preventive maintenance tracking.

The app is designed for internal operational use in hospitals, offices, schools, or other organizations that manage many IT devices.

---

# Main Goals

- Simple and modern UI
- Fast workflow for technicians
- Easy asset lookup using QR code
- Maintenance and repair documentation
- Offline-first functionality
- Clean operational history tracking

---

# Tech Stack

- Kotlin
- Jetpack Compose
- MVVM Architecture
- Room Database
- Firebase Firestore
- Coroutines + Flow
- Material 3

---

# Current Main Features

## Asset Inventory
- Register new IT assets
- Inventory number management
- Device information
- Location management
- Status management

## Repair Registration
- Register damaged devices
- Track repair process
- Before/after documentation photos
- Technician notes
- Repair history

## Preventive Maintenance
- Routine maintenance registration
- Maintenance documentation
- Maintenance history

## Image Watermark
Photos automatically receive watermark information:
- Date
- Time
- Location

---

# Current Project Status

- APK builds successfully
- Main application is stable
- QR scanner integration currently in progress

---

# QR Scanner Integration Status

Current progress:
- Google Play Services Code Scanner dependency already added
- Fake scanner dialog already exists
- Scanner UI state already exists

Still needed:
- Add CAMERA permission
- Replace fake scanner with real scanner
- Autofill inventory number after scan
- Close scanner automatically after successful scan

---

# Latest Requested Features

## Real QR Code Scanner
Requirements:
- Use phone camera directly
- Use Google Code Scanner API
- Scan QR code and automatically fill inventory number input
- Remove fake scanner simulation

---

## Asset Detail Screen Revision

### Rename Button
- Change button text from:
  "Lapor Rusak"
  to:
  "Perbaikan"

### Add New Button
- Add new button:
  "Update Aset"

### Update Asset Fields
Fields that can be updated:
- Location
- Status
- Specifications
- Additional notes

### Status Options
- Aktif
- Rusak Permanen

If "Rusak Permanen" selected:
- Require reason input
- Require permanent damage explanation

### Auto Timestamp
When saving asset updates:
- Automatically use current phone date and time

### Asset Update History
Add update history tracking:
- Asset update history
- Repair history
- Routine maintenance history

All histories must be visible inside asset detail information screen.

---

# Repair and Maintenance Flow Improvement

Current issue:
When opening repair or maintenance form from asset detail screen, inventory number still must be manually typed.

Required behavior:
- Automatically use currently selected asset inventory number
- Do not require manual inventory number input again

---

# Camera Workflow Revision

Current issue:
Photo input still allows:
- Gallery upload
- Visual simulation preset

Required behavior:
- Remove gallery upload option
- Remove visual simulation option
- Camera must open directly
- Photo automatically saved with watermark

Still required:
- Delete photo option
- Retake photo option

Applies to:
- Before photo
- After photo
- Device photo

---

# New Asset Registration Revision

Add new fields:
- Procurement date
- Purchase price

---

# UI Direction

Preferred UI style:
- Clean
- Simple
- Modern
- Efficient
- Fast workflow
- Minimal unnecessary dialogs

---

# Important Notes

- Keep existing architecture clean
- Keep Compose UI structure modern
- Avoid breaking current stable APK build
- Maintain compatibility with current Room database structure when possible
