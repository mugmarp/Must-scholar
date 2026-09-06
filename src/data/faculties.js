import { Stethoscope, FlaskConical, Cog, Cpu, Briefcase, Users } from "lucide-react";

// Official MUST faculty & programme structure (source: must.ac.ug undergraduate programmes)
export const FACULTIES = [
  {
    id: "medicine",
    name: "Faculty of Medicine",
    subtitle: "Faculty of Health Sciences",
    icon: Stethoscope,
    programmes: [
      { code: "MBR", name: "Bachelor of Medicine and Bachelor of Surgery", years: 5 },
      { code: "PHA", name: "Bachelor of Pharmacy", years: 5 },
      { code: "BNS", name: "Bachelor of Nursing Science", years: 4 },
      { code: "MLS", name: "Bachelor of Medical Laboratory Science", years: 4 },
      { code: "BSP", name: "Bachelor of Science in Physiotherapy", years: 4 },
      { code: "PHS", name: "Bachelor of Science in Pharmaceutical Sciences", years: 3 },
      { code: "DCM", name: "Diploma in Community HIV/AIDS Care and Management", years: 2 },
      { code: "DEM", name: "Diploma in Emergency Medicine", years: 2 },
      { code: "DCAM", name: "Adv. Diploma in Child and Adolescent Mental Health", years: 2 },
    ],
  },
  {
    id: "science",
    name: "Faculty of Science",
    subtitle: null,
    icon: FlaskConical,
    programmes: [
      { code: "BS", name: "Bachelor of Science with Education (Physical / Biological / Chemical)", years: 3 },
      { code: "DLT", name: "Diploma in Science Laboratory Technology", years: 2 },
    ],
  },
  {
    id: "fast",
    name: "Faculty of Applied Sciences and Technology",
    subtitle: "FAST",
    icon: Cog,
    programmes: [
      { code: "BME", name: "Bachelor of Biomedical Engineering", years: 4 },
      { code: "EEE", name: "Bachelor of Engineering in Electrical & Electronics Engineering", years: 4 },
      { code: "PEEM", name: "BSc in Petroleum Engineering & Environmental Management", years: 4 },
      { code: "CVE", name: "Bachelor of Science in Civil Engineering", years: 4 },
      { code: "MIE", name: "Bachelor of Science in Mechanical and Industrial Engineering", years: 4 },
    ],
  },
  {
    id: "computing",
    name: "Faculty of Computing and Informatics",
    subtitle: null,
    icon: Cpu,
    programmes: [
      { code: "BCS", name: "Bachelor of Computer Science", years: 3 },
      { code: "BIT", name: "Bachelor of Information Technology", years: 3 },
      { code: "BSE", name: "Bachelor of Software Engineering", years: 4 },
    ],
  },
  {
    id: "business",
    name: "Faculty of Business and Management Sciences",
    subtitle: null,
    icon: Briefcase,
    programmes: [
      { code: "BBA", name: "Bachelor of Business Administration", years: 3 },
      { code: "BSAF", name: "Bachelor of Science in Accounting and Finance", years: 3 },
      { code: "ECO", name: "Bachelor of Science in Economics", years: 3 },
      { code: "BPSM", name: "BSc in Procurement & Supply Chain Management", years: 3 },
    ],
  },
  {
    id: "interdisciplinary",
    name: "Faculty of Interdisciplinary Studies",
    subtitle: null,
    icon: Users,
    programmes: [
      { code: "BSAL", name: "BSc in Agriculture and Livelihoods", years: 4 },
      { code: "BGWH", name: "BSc in Gender and Applied Women Health", years: 3 },
      { code: "BPCD", name: "BSc in Planning and Community Development", years: 3 },
    ],
  },
];