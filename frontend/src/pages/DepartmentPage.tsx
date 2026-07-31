/* eslint-disable react-hooks/set-state-in-effect */
/**
 * 部署管理ページ
 */

import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { departmentApi } from '../features/master/masterApi';

import type {
    CreateDepartmentRequest,
    UpdateDepartmentRequest,
    Department
} from '../features/master/types';
import { MasterSelector } from '../components/masterPage/MasterSelector';
import { MasterTable } from '../components/masterPage/MasterTable';
import { MasterButtonArea } from '../components/masterPage/MasterButonArea';
import { ROUTES } from '../constants/routes';
import { canDeleteHistory, isActiveHistory, sortMasterHistory } from '../components/masterPage/MasterUtils';
import { MASTER_MESSAGES } from '../features/master/messages';

export default function DepartmentPage() {
    const navigate = useNavigate();
    const [departments, setDepartments] = useState<Department[]>([]);
    const [keyword, setKeyword] = useState('');
    const [selected, setSelected] = useState<Department | null>(null);
    const [departmentName, setDepartmentName] = useState('');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [message, setMessage] = useState('');
    const [isCreateMode, setIsCreateMode] = useState(false);

    const loadDepartments = async () => {

        try {
            const data = await departmentApi.findAll();
            setDepartments(data);
        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.dataFetchFailed
            );
        }
    };

    useEffect(() => {
        loadDepartments();
    }, []);

    const filteredDepartments =
        useMemo(() => {

            const filtered =
                !keyword.trim()
                    ? departments
                    : departments.filter(
                        department =>
                            department.departmentName.includes(
                                keyword,
                            ),
                    );

            return sortMasterHistory(
                filtered,
                row => row.departmentId,
            );

        }, [departments, keyword]);

    const handleSelect = (
        department: Department,
    ) => {
        setSelected(department);
        setIsCreateMode(false);
        setDepartmentName(department.departmentName);
        setStartDate(department.startDate);
        setEndDate(department.endDate ?? '');
        setMessage('');
    };

    const handleNew = () => {
        setSelected(null);
        setIsCreateMode(true);
        setDepartmentName('');
        setStartDate('');
        setEndDate('');
    };

    const handleCreate = async () => {
        try {
            const request: CreateDepartmentRequest = {
                departmentName,
                startDate,
            };

            await departmentApi.createDepartment(request);
            await loadDepartments();
            setMessage(MASTER_MESSAGES.createSuccess);
            handleNew();
        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.createFailed);
        }
    };

    const handleUpdate = async () => {
        if (!selected) {
            return;
        }
        try {
            const request: UpdateDepartmentRequest = {
                departmentId: selected.departmentId,
                departmentName,
                startDate,
            };

            await departmentApi.updateDepartment(request);
            await loadDepartments();
            setMessage(MASTER_MESSAGES.updateSuccess);
        } catch (error) {
            setMessage(error instanceof Error
                ? error.message
                : MASTER_MESSAGES.updateSuccess);
        }
    };

    const handleDelete = async () => {
        if (!selected) {
            return;
        }
        if (!window.confirm(MASTER_MESSAGES.deleteConfirm)) {
            return;
        }

        try {
            await departmentApi.deleteDepartment(
                selected.departmentId,
                selected.startDate);

            await loadDepartments();
            handleNew();
            setMessage(MASTER_MESSAGES.deleteSuccess);
        } catch (error) {
            setMessage(error instanceof Error
                ? error.message
                : MASTER_MESSAGES.deleteFailed,
            );
        }
    };

    const handleClear = () => {
        setKeyword('');
        setDepartmentName('');
        setStartDate('');
        setEndDate('');
        setSelected(null);
        setIsCreateMode(false);
        setMessage('');
    };

    return (
        <div className="department-page">

            <div className="page-header">
                <h1>マスタデータ管理画面</h1>
            </div>

            <div className="search-area">

                <div className="search-item">
                    <label>管理対象マスタ</label>

                    <MasterSelector value='department' />
                </div>

                <div className="search-item">
                    <label>検索条件</label>

                    <input
                        type="text"
                        value={keyword}
                        placeholder="名称で検索"
                        onChange={e =>
                            setKeyword(e.target.value)
                        }
                    />
                </div>

                <button onClick={handleNew}>
                    新規作成
                </button>

            </div>

            <div className="content-area">

                <div className="master-panel">

                    <div className="panel-header">
                        マスター一覧
                    </div>

                    <div className="panel-body">
                        <MasterTable
                            rows={filteredDepartments}
                            onSelect={handleSelect}
                            columns={[
                                {
                                    header: 'ID',
                                    render: row =>
                                        row.departmentId
                                },
                                {
                                    header: '部署名',
                                    render: row =>
                                        row.departmentName
                                },
                                {
                                    header: '適用期間',
                                    render: row =>
                                        `${row.startDate} ～ ${row.endDate ?? ''}`
                                },
                                {
                                    header: '状態',
                                    render: row =>
                                        isActiveHistory(
                                            row.startDate,
                                            row.endDate)
                                            ? '有効'
                                            : '無効'
                                }
                            ]}
                        />
                    </div>
                </div>

                <div className="master-panel">
                    <div className="panel-header">
                        詳細入力エリア
                    </div>
                    <div className="panel-body">
                        <div className="form-row">
                            <label>部署名</label>
                            <input
                                value={departmentName}
                                onChange={e =>
                                    setDepartmentName(
                                        e.target.value
                                    )
                                }
                            />
                        </div>

                        <div className="form-row">
                            <label>適用開始日</label>

                            <input
                                type="date"
                                value={startDate}
                                onChange={e =>
                                    setStartDate(
                                        e.target.value
                                    )
                                }
                            />
                        </div>

                        <div className="form-row">
                            <label>適用終了日</label>

                            <input
                                value={endDate}
                                readOnly
                            />
                        </div>

                        <MasterButtonArea
                            onCreate={handleCreate}
                            onUpdate={handleUpdate}
                            onDelete={handleDelete}
                            onClear={handleClear}
                            onBack={() => navigate(ROUTES.menu)}

                            createDisabled={
                                !isCreateMode ||
                                departmentName == '' ||
                                startDate == ''
                            }
                            updateDisabled={
                                !selected ||
                                selected.endDate !== null ||
                                departmentName == '' ||
                                startDate == ''
                            }
                            deleteDisabled={
                                !selected ||
                                !canDeleteHistory(
                                    selected.startDate,
                                    selected.endDate
                                )
                            }
                        />
                    </div>
                </div>
            </div>

            <div className="message-area">
                {message}
            </div>

        </div>
    );
}